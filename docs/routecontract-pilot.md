# Proposed database-strategy regression check

This opt-in draft tests the starter's `database-strategy` property/configuration
wiring. It replaces the previous single-source `plain_table` proposal, which did
not demonstrate enough starter-specific value beyond the existing insert test.
It does not change the published starter or its production dependencies.

## What it checks

Two synthetic H2 databases contain the same `t_order` row. The starter is given
an inline database strategy on `id`. A prepared query with `id = 1` and `LIMIT 1`
must return `[1]` and report one physical JDBC execution attempt.

If the starter stops applying `setDatabaseShardingStrategy`, the returned value
is still `[1]`, but the query broadcasts and reports two attempts. The existing
five tests still pass with that seeded mutation. The proposed test checks the
returned row first, then capture completeness, reported failures and the count.
This is a controlled regression test, not a newly discovered production bug.

To reproduce the mutation locally, temporarily remove only the two-line
`tableRuleConfiguration.setDatabaseShardingStrategy(...)` call in
`ShardingsphereJdbcAutoConfiguration.dataSource()`, leaving algorithm creation
intact. Run `clean test routeContractPilotTest -ProutecontractPilot=true` through
the wrapper for either Boot version below. The five existing tests pass; the new
test prints `result=[1] observedAttempts=2` and fails with expected 1 / observed 2.
Restore that call before committing or running the normal proposal again.

The expectation comes from the supplied database strategy. It does not pin the
internal data-source name, a serialized snapshot or candidate bytes. Observed
attempts are not physical-table counts, complete route plans or commit proof.
The duplicated fixture row makes the result-only blind spot deliberate; it is
not a recommended production data layout. A backing-JDBC counter could test the
same invariant; RouteContract supplies the scoped assertion API here.

## Run

Use JDK 21, the repository's Gradle wrapper and ShardingSphere-JDBC 5.5.3:

```sh
./gradlew --no-daemon clean build
./gradlew --no-daemon --dependency-verification=strict routeContractPilotTest \
  -ProutecontractPilot=true -PspringBootVersion=3.5.16
./gradlew --no-daemon --dependency-verification=strict routeContractPilotTest \
  -ProutecontractPilot=true -PspringBootVersion=4.1.0
```

The default build has no pilot task or RouteContract dependency. The existing CI
matrix runs the opt-in task in a separate step after each normal build. There is
no separate workflow, shell verifier or uploaded candidate artifact.

RouteContract 0.1.3 resolves through Maven Central with its normal POM dependencies.
Maven local excludes only the RouteContract group. [Gradle's native verification](https://docs.gradle.org/current/userguide/dependency_verification.html)
checks the added RouteContract JAR against one reviewed checksum. Metadata,
signatures and other dependency artifacts are outside this small check; they
retain the base build's trust model. This is not full dependency-graph verification.
There are no pilot lockfiles or custom artifact-origin/CodeSource checks.

## Dependency upgrades

1. Update the dependency as usual and run both normal builds and the opt-in test.
   A Boot or H2 upgrade does not require a new candidate or lockfile.
2. For a RouteContract upgrade, review the published artifact and update its
   version and single Gradle checksum together. Do not accept a new checksum just
   because Gradle observed it locally.
3. ShardingSphere versions other than 5.5.3 are explicitly rejected by this pilot
   until compatibility is separately established.
4. If an intentional upgrade changes execution behavior, inspect whether the
   configured targeted query still avoids broadcast. Explain any change to the
   semantic assertion in a normal reviewed PR, or remove the pilot if it no
   longer provides value. Do not increase the budget solely to turn a test green.

No manifest baseline is generated, added or approved. A passing test establishes
only these proposed H2 assertions. The PR stays in Draft while the maintainer
decides whether this smaller, changed scope merits inclusion.
