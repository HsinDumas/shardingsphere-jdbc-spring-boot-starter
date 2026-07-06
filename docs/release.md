# Release Guide

This project uses tag-driven release with GitHub Actions.

## Overview

1. Push tag `vX.Y.Z`
2. GitHub Actions workflow publishes to Maven Central
3. GitHub Release notes are generated automatically
4. `projectVersion` in `gradle.properties` is bumped to next `-SNAPSHOT`

## Prerequisites

Configure repository secrets:

1. `MAVEN_CENTRAL_USERNAME`
2. `MAVEN_CENTRAL_PASSWORD`
3. `SIGNING_IN_MEMORY_KEY`
4. `SIGNING_IN_MEMORY_KEY_PASSWORD`

## Local Commands

```bash
# Build and test
./gradlew clean build

# Print effective version
./gradlew printVersion

# Publish snapshot
./gradlew publishSnapshotToMavenCentral
```

Release publication is restricted to CI tag builds.

## Official Release Steps

```bash
# 1) Ensure you are on latest main
git checkout main
git pull --ff-only origin main

# 2) Create and push release tag
git tag v1.0.4
git push origin v1.0.4
```

## CI Workflows

1. `.github/workflows/ci.yml`: matrix build/tests for Spring Boot 3 and 4
2. `.github/workflows/release.yml`: release publication and post-release snapshot bump

## Troubleshooting

1. `Version already exists on Maven Central`: choose a new version tag.
2. Signing failure: verify `SIGNING_IN_MEMORY_KEY` and `SIGNING_IN_MEMORY_KEY_PASSWORD`.
3. Maven Central auth failure: verify `MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD`.
4. Tag mismatch failure: ensure `releaseVersion` resolved from tag exactly matches `vX.Y.Z`.
