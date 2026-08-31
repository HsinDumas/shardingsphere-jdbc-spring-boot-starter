#!/usr/bin/env bash
set -uo pipefail

die() { printf 'routecontract-review-state: %s\n' "$*" >&2; exit 1; }
file_sha() { python3 -c 'import hashlib,sys; print(hashlib.sha256(open(sys.argv[1], "rb").read()).hexdigest())' "$1"; }
lstat_identity() { python3 -c 'import os,sys; s=os.lstat(sys.argv[1]); print(f"{s.st_dev}:{s.st_ino}:{s.st_mode}:{s.st_size}:{s.st_mtime_ns}")' "$1"; }
boot_version=${1:?Spring Boot version is required}
case "$boot_version" in 3.5.16|4.1.0) ;; *) die "unsupported Spring Boot version: $boot_version" ;; esac

baseline=src/routeContractPilot/resources/route-contracts/plain-table.insert.json
candidate=build/routecontract/plain-table.insert.candidate.json
report_dir=build/test-results/routeContractPilotTest
temp_root=${ROUTECONTRACT_TEMP_ROOT:-${RUNNER_TEMP:-${TMPDIR:-/tmp}}}
[[ -d "$temp_root" && ! -L "$temp_root" ]] || die "temporary root must be an existing regular directory"
gradle_home=$(mktemp -d "$temp_root/routecontract-gradle-${boot_version}.XXXXXX") || die "could not create fresh GRADLE_USER_HOME"
cleanup() { rm -rf -- "$gradle_home"; }
trap cleanup EXIT
[[ ! -e "$candidate" && ! -L "$candidate" ]] || die "stale candidate exists before run"
[[ ! -e "$report_dir" && ! -L "$report_dir" ]] || die "stale JUnit report exists before run"
export GRADLE_USER_HOME="$gradle_home"

baseline_sha=
baseline_identity=
if [[ -L "$baseline" ]]; then
  die "baseline must not be a symlink"
elif [[ -e "$baseline" ]]; then
  [[ -f "$baseline" ]] || die "baseline must be a regular file"
  mode=match
  baseline_sha=$(file_sha "$baseline") || die "could not hash baseline before MATCH"
  baseline_identity=$(lstat_identity "$baseline") || die "could not lstat baseline before MATCH"
else
  mode=review
fi

set +e
./gradlew --no-daemon routeContractPilotTest -PspringBootVersion="$boot_version" -ProutecontractPilot=true
gradle_rc=$?
set -e
[[ -f "$candidate" && ! -L "$candidate" ]] || die "candidate is missing, nonregular, or a symlink"
mapfile -t reports < <(find "$report_dir" -maxdepth 1 -type f -name 'TEST-*.xml' -print)
[[ ${#reports[@]} -eq 1 ]] || die "expected exactly one JUnit XML, found ${#reports[@]}"
expected_report="$report_dir/TEST-com.github.dumas.shardingsphere.jdbc.RouteContractPlainTablePilotTest.xml"
[[ "${reports[0]}" == "$expected_report" ]] || die "unexpected JUnit XML: ${reports[0]}"

if ! python3 - "$mode" "$gradle_rc" "${reports[0]}" <<'PY'
import sys
import xml.etree.ElementTree as ET
mode, rc_text, report = sys.argv[1:]
rc = int(rc_text)
suite = ET.parse(report).getroot()
count_keys = ("tests", "failures", "errors", "skipped")
missing_count_keys = [key for key in count_keys if key not in suite.attrib]
if missing_count_keys:
    raise SystemExit(f"missing JUnit count attributes: {missing_count_keys}")
counts = {key: int(suite.attrib[key]) for key in count_keys}
failures = suite.findall("./testcase/failure")
error_nodes = suite.findall("./testcase/error")
skipped_nodes = suite.findall("./testcase/skipped")
cases = suite.findall("./testcase")
missing = "org.opentest4j.AssertionFailedError: No human-approved baseline. Review build/routecontract/plain-table.insert.candidate.json"
suite_name = "com.github.dumas.shardingsphere.jdbc.RouteContractPlainTablePilotTest"
case_name = "writesCandidateThenStopsForHumanBaselineApproval()"
def require(condition, message):
    if not condition:
        raise SystemExit(message)
require(suite.tag == "testsuite" and suite.attrib.get("name") == suite_name, f"unexpected suite: {suite.attrib}")
require(len(cases) == 1, f"expected one testcase, found {len(cases)}")
require(cases[0].attrib.get("classname") == suite_name and cases[0].attrib.get("name") == case_name,
        f"unexpected testcase: {cases[0].attrib}")
require(not error_nodes and not skipped_nodes, "JUnit XML contains error or skipped child nodes")
if mode == "review":
    require(rc != 0, f"REVIEW must have nonzero Gradle rc, got {rc}")
    require(counts == {"tests": 1, "failures": 1, "errors": 0, "skipped": 0}, str(counts))
    require(len(failures) == 1 and failures[0].attrib.get("message") == missing,
            repr([f.attrib for f in failures]))
    require(failures[0].attrib.get("type") == "org.opentest4j.AssertionFailedError",
            f"unexpected failure type: {failures[0].attrib.get('type')}")
else:
    require(rc == 0, f"MATCH must have zero Gradle rc, got {rc}")
    require(counts == {"tests": 1, "failures": 0, "errors": 0, "skipped": 0}, str(counts))
    require(not failures, repr([f.attrib for f in failures]))
PY
then
  die "JUnit review-state verification failed"
fi
candidate_sha=$(file_sha "$candidate") || die "could not hash candidate after report verification"
[[ "$candidate_sha" =~ ^[0-9a-f]{64}$ ]] || die "candidate SHA-256 is not fixed lowercase hex"
report_sha=$(file_sha "${reports[0]}") || die "could not hash verified JUnit report"
[[ "$report_sha" =~ ^[0-9a-f]{64}$ ]] || die "JUnit report SHA-256 is not fixed lowercase hex"

if [[ "$mode" == review ]]; then
  [[ ! -e "$baseline" && ! -L "$baseline" ]] || die "baseline appeared during REVIEW"
  artifact_ready=true
else
  [[ -f "$baseline" && ! -L "$baseline" ]] || die "baseline changed type during MATCH"
  baseline_sha_after=$(file_sha "$baseline") || die "could not hash baseline after MATCH"
  baseline_identity_after=$(lstat_identity "$baseline") || die "could not lstat baseline after MATCH"
  [[ "$baseline_sha_after" == "$baseline_sha" ]] || die "baseline bytes changed during MATCH"
  [[ "$candidate_sha" == "$baseline_sha" ]] || die "MATCH candidate bytes differ from baseline bytes"
  [[ "$baseline_identity_after" == "$baseline_identity" ]] || die "baseline identity changed during MATCH"
  artifact_ready=false
fi
if [[ -n ${GITHUB_OUTPUT:-} ]]; then
  printf 'mode=%s\nartifact_ready=%s\ncandidate_sha256=%s\nreport_sha256=%s\n' \
    "$mode" "$artifact_ready" "$candidate_sha" "$report_sha" >> "$GITHUB_OUTPUT"
fi
printf 'ROUTECONTRACT_REVIEW_STATE mode=%s artifact_ready=%s boot=%s candidate_sha256=%s report_sha256=%s\n' \
  "$mode" "$artifact_ready" "$boot_version" "$candidate_sha" "$report_sha"
