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
val javaToolchainVersion = 25
val javaReleaseVersion = 17

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaToolchainVersion)
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    api("org.apache.shardingsphere:shardingsphere-jdbc:$shardingsphereVersion")
    implementation("org.apache.shardingsphere:shardingsphere-sharding-core:$shardingsphereVersion")
    implementation("org.apache.shardingsphere:shardingsphere-standalone-mode-core:$shardingsphereVersion")
    implementation("org.apache.shardingsphere:shardingsphere-standalone-mode-repository-api:$shardingsphereVersion")
    implementation("org.apache.shardingsphere:shardingsphere-standalone-mode-repository-memory:$shardingsphereVersion")
    implementation("org.apache.shardingsphere:shardingsphere-infra-data-source-pool-hikari:$shardingsphereVersion")
    implementation("com.zaxxer:HikariCP")

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
        description = "适用于 shardingsphere-jdbc 的 spring-boot-starter (Spring Boot 3.x)"
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
