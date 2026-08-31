import java.security.MessageDigest
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.dsl.LockMode
import org.gradle.api.artifacts.result.ResolvedDependencyResult

plugins {
    `java-library`
    id("com.vanniktech.maven.publish") version "0.37.0"
}

group = providers.gradleProperty("group").get()
val artifactId = providers.gradleProperty("artifactId").get()
val fallbackVersion = providers.gradleProperty("projectVersion")
    .orElse("1.0.5-SNAPSHOT")
val resolvedVersion = providers.gradleProperty("releaseVersion")
    .orElse(providers.environmentVariable("RELEASE_VERSION"))
    .orElse(fallbackVersion)
version = resolvedVersion.get()

val springBootVersion = providers.gradleProperty("springBootVersion").get()
val shardingsphereVersion = providers.gradleProperty("shardingsphereVersion").get()
val javaToolchainVersion = 21
val javaReleaseVersion = 17
val routecontractPilotEnabled = providers.gradleProperty("routecontractPilot").orNull == "true"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaToolchainVersion)
    }
}

repositories {
    if (!routecontractPilotEnabled) {
        mavenLocal()
    }
    mavenCentral()
}

dependencies {
    api("org.apache.shardingsphere:shardingsphere-jdbc:$shardingsphereVersion")
    implementation("org.apache.shardingsphere:shardingsphere-sharding-core:$shardingsphereVersion")
    implementation("org.apache.shardingsphere:shardingsphere-parser-sql-engine-mysql:$shardingsphereVersion")
    implementation("org.apache.shardingsphere:shardingsphere-authority-simple:$shardingsphereVersion")
    implementation("org.apache.shardingsphere:shardingsphere-standalone-mode-core:$shardingsphereVersion")
    implementation("org.apache.shardingsphere:shardingsphere-standalone-mode-repository-api:$shardingsphereVersion")
    implementation("org.apache.shardingsphere:shardingsphere-standalone-mode-repository-jdbc:$shardingsphereVersion")
    implementation("org.apache.shardingsphere:shardingsphere-infra-data-source-pool-hikari:$shardingsphereVersion")

    compileOnly(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    annotationProcessor(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    compileOnly("org.springframework.boot:spring-boot-starter")
    compileOnly("org.springframework.boot:spring-boot-starter-validation")

    compileOnly("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaReleaseVersion)
}

tasks.test {
    useJUnitPlatform()
}

if (routecontractPilotEnabled) {
    val releaseBaseUrl = providers.gradleProperty("routecontractReleaseBaseUrl")
        .orElse("https://github.com/ym0506/routecontract/releases/download/v0.1.2")
        .get()
    val officialReleaseBaseUrl = "https://github.com/ym0506/routecontract/releases/download/v0.1.2"
    val allowTestUrlOverride = providers.gradleProperty("routecontractTestAllowUrlOverride").orNull == "true"
    require(releaseBaseUrl == officialReleaseBaseUrl || allowTestUrlOverride) {
        "routecontractReleaseBaseUrl must be the immutable v0.1.2 GitHub Release URL"
    }
    val officialSha256 = "d25cd2699629890db7195e871461b25861991fe20abd776d702c690a292b72fc"
    val suppliedSha256 = providers.gradleProperty("routecontractExpectedSha256").orNull
    val allowTestShaOverride = providers.gradleProperty("routecontractTestAllowShaOverride").orNull == "true"
    require(suppliedSha256 == null || suppliedSha256 == officialSha256 || allowTestShaOverride) {
        "routecontractExpectedSha256 override is test-only; immutable v0.1.2 SHA is fixed"
    }
    val expectedSha256 = suppliedSha256 ?: officialSha256

    repositories {
        exclusiveContent {
            forRepository {
                ivy {
                    name = "routecontractV012ImmutableRelease"
                    url = uri(releaseBaseUrl)
                    patternLayout { artifact("[artifact]-[revision].[ext]") }
                    metadataSources { artifact() }
                }
            }
            filter { includeModule("io.github.ym0506.routecontract", "routecontract-shardingsphere-5.5") }
        }
    }

    // Reuse the existing plain_table test operation and setup so the review-only pilot cannot drift from it.
    val testSourceSet = sourceSets.test.get()
    val routeContractPilot by sourceSets.creating {
        java.srcDir("src/routeContractPilot/java")
        compileClasspath += sourceSets.main.get().output
        compileClasspath += testSourceSet.output
        runtimeClasspath += sourceSets.main.get().output
        runtimeClasspath += testSourceSet.output
    }
    configurations[routeContractPilot.implementationConfigurationName]
        .extendsFrom(configurations.testImplementation.get())
    configurations[routeContractPilot.runtimeOnlyConfigurationName]
        .extendsFrom(configurations.testRuntimeOnly.get())
    // The two supported Boot graphs differ, so each gets a complete lock for every resolvable pilot task input.
    dependencyLocking {
        lockMode.set(LockMode.STRICT)
        lockFile.set(layout.projectDirectory.file("gradle/routecontract-pilot-$springBootVersion.lockfile"))
    }
    configurations.configureEach {
        if (isCanBeResolved) {
            resolutionStrategy.activateDependencyLocking()
        }
    }
    dependencies {
        add(routeContractPilot.implementationConfigurationName,
            "io.github.ym0506.routecontract:routecontract-shardingsphere-5.5:0.1.2@jar") {
            isTransitive = false
        }
        add(routeContractPilot.runtimeOnlyConfigurationName, "com.alibaba:transmittable-thread-local") {
            version { strictly("2.14.2") }
        }
        add(routeContractPilot.runtimeOnlyConfigurationName, "tools.jackson.core:jackson-core") {
            version { strictly("3.1.5") }
        }
    }

    val verifyRoutecontractArtifact by tasks.registering {
        val pilotConfiguration = configurations[routeContractPilot.runtimeClasspathConfigurationName]
        inputs.files(pilotConfiguration)
        doLast {
            val edge = pilotConfiguration.incoming.resolutionResult.root.dependencies
                .filterIsInstance<ResolvedDependencyResult>()
                .single { dependency ->
                    val requested = dependency.requested
                    !dependency.isConstraint
                            && requested is ModuleComponentSelector
                            && requested.group == "io.github.ym0506.routecontract"
                }
            val requested = edge.requested as ModuleComponentSelector
            check(requested.module == "routecontract-shardingsphere-5.5" && requested.version == "0.1.2") {
                "Requested RouteContract selector changed: $requested"
            }
            val selected = edge.selected.id as? ModuleComponentIdentifier
                ?: error("Selected RouteContract component is not a module: ${edge.selected.id}")
            check(selected.group == requested.group && selected.module == requested.module && selected.version == requested.version) {
                "Requested-to-selected RouteContract edge changed: requested=$requested selected=$selected"
            }
            val artifacts = pilotConfiguration.resolvedConfiguration.resolvedArtifacts.filter {
                it.moduleVersion.id.group == "io.github.ym0506.routecontract"
            }
            check(artifacts.size == 1) { "Expected one RouteContract artifact, found $artifacts" }
            val artifact = artifacts.single()
            check(artifact.id.componentIdentifier == selected) {
                "RouteContract artifact is not bound to the selected component"
            }
            check(artifact.file.name == "routecontract-shardingsphere-5.5-0.1.2.jar") {
                "Unexpected RouteContract artifact: ${artifact.file.name}"
            }
            val actualSha = MessageDigest.getInstance("SHA-256")
                .digest(artifact.file.readBytes()).joinToString("") { "%02x".format(it) }
            check(actualSha == expectedSha256) {
                "RouteContract JAR SHA-256 mismatch: expected=$expectedSha256 actual=$actualSha"
            }
            fun requireExactRuntimeComponent(group: String, module: String, version: String) {
                val components = pilotConfiguration.incoming.resolutionResult.allComponents.mapNotNull {
                    it.id as? ModuleComponentIdentifier
                }.filter { it.group == group && it.module == module }
                check(components.size == 1 && components.single().version == version) {
                    "Runtime component changed: expected=$group:$module:$version selected=$components"
                }
                check(pilotConfiguration.resolvedConfiguration.resolvedArtifacts.any {
                    it.id.componentIdentifier == components.single()
                }) { "No artifact is bound to runtime component ${components.single()}" }
            }
            requireExactRuntimeComponent("com.alibaba", "transmittable-thread-local", "2.14.2")
            requireExactRuntimeComponent("tools.jackson.core", "jackson-core", "3.1.5")
            project.extensions.extraProperties["routecontractVerifiedJar"] = artifact.file.absolutePath
            println("ROUTECONTRACT_VERIFIED requested=$requested selected=$selected sha256=$actualSha file=${artifact.file}")
        }
    }
    tasks.named<JavaCompile>(routeContractPilot.compileJavaTaskName) {
        dependsOn(verifyRoutecontractArtifact)
        dependsOn(tasks.named(testSourceSet.classesTaskName))
        javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(17)) })
        options.release.set(17)
    }
    tasks.register<Test>("routeContractPilotTest") {
        dependsOn(verifyRoutecontractArtifact, routeContractPilot.classesTaskName)
        testClassesDirs = routeContractPilot.output.classesDirs
        classpath = routeContractPilot.runtimeClasspath
        javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
        useJUnitPlatform()
        systemProperty("routecontract.projectDir", layout.projectDirectory.asFile.absolutePath)
        systemProperty("routecontract.expectedSha256", expectedSha256)
    }
}

fun Project.enforceTagDrivenRelease() {
    val isCi = providers.environmentVariable("CI").orNull == "true"
    val refType = providers.environmentVariable("GITHUB_REF_TYPE").orNull
    val refName = providers.environmentVariable("GITHUB_REF_NAME").orNull
    val releaseVersion = providers.gradleProperty("releaseVersion")
        .orElse(providers.environmentVariable("RELEASE_VERSION"))
        .orNull

    if (!isCi || refType != "tag" || refName.isNullOrBlank()) {
        throw GradleException(
            "Release publishing is restricted to GitHub Actions tag builds (push tag vX.Y.Z)."
        )
    }

    if (releaseVersion.isNullOrBlank()) {
        throw GradleException(
            "releaseVersion must be provided from tag workflow and match the pushed tag version."
        )
    }

    val expectedVersion = refName.removePrefix("v")
    if (releaseVersion != expectedVersion) {
        throw GradleException(
            "releaseVersion ($releaseVersion) does not match tag version ($expectedVersion)."
        )
    }
}

gradle.taskGraph.whenReady {
    val releasePublishRequested = allTasks.any {
        it.name == "publishReleaseToMavenCentral" || it.name == "publishAndReleaseToMavenCentral"
    }
    if (releasePublishRequested) {
        project.enforceTagDrivenRelease()
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    coordinates(project.group.toString(), artifactId, project.version.toString())

    pom {
        name = "shardingsphere-jdbc-spring-boot-starter"
        description = "适用于 shardingsphere-jdbc 的 spring-boot-starter (Spring Boot 3.x / 4.x)"
        url = "https://github.com/HsinDumas/shardingsphere-jdbc-spring-boot-starter"

        licenses {
            license {
                name = "Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }

        developers {
            developer {
                id = "HsinDumas"
                name = "HsinDumas"
                email = "HsinDumas@gmail.com"
            }
        }

        scm {
            connection = "scm:git:git://github.com/HsinDumas/shardingsphere-jdbc-spring-boot-starter.git"
            developerConnection = "scm:git:ssh://git@github.com/HsinDumas/shardingsphere-jdbc-spring-boot-starter.git"
            url = "https://github.com/HsinDumas/shardingsphere-jdbc-spring-boot-starter"
        }
    }
}

tasks.matching { it.name == "generateMetadataFileForMavenPublication" }.configureEach {
    dependsOn("plainJavadocJar")
}

tasks.register("printVersion") {
    group = "help"
    description = "Prints the current project version"
    doLast {
        println(project.version)
    }
}

tasks.register("publishSnapshotToMavenCentral") {
    group = "publishing"
    description = "Publishes snapshot artifacts to Sonatype Central snapshot repository"
    dependsOn("publishToMavenCentral")
}

tasks.register("publishReleaseToMavenCentral") {
    group = "publishing"
    description = "Publishes and releases artifacts to Maven Central"
    dependsOn("publishAndReleaseToMavenCentral")
}
