#!/usr/bin/env bash
# Real Android evidence on the disposable CI emulator, never a personal device.
set -euo pipefail
cd "$(dirname "$0")/../.."
mkdir -p device_artifacts/cases
printf 'Scripted screen-coordinate Android tests, not unscripted human play.\nEach clip records one complete bounded test; inspect result.tsv for failures.\n' > device_artifacts/README.txt
git rev-parse HEAD > device_artifacts/commit.txt
adb -e get-state
gradle :app:assembleDebug :app:assembleDebugAndroidTest --stacktrace --no-daemon
mapfile -t APKS < <(find app/build/outputs/apk/debug -maxdepth 1 -name '*.apk')
mapfile -t TEST_APKS < <(find app/build/outputs/apk/androidTest/debug -maxdepth 1 -name '*.apk')
[[ ${#APKS[@]} -eq 1 && ${#TEST_APKS[@]} -eq 1 ]]
adb -e install -r "${APKS[0]}"
adb -e install -r "${TEST_APKS[0]}"
adb -e logcat -c
RECORDER_PID=""
CURRENT_REMOTE=""
CURRENT_LOCAL=""
stop_recording() {
    if [[ -n "$RECORDER_PID" ]]; then
        adb -e shell pkill -INT screenrecord >/dev/null 2>&1 || true
        wait "$RECORDER_PID" || true
        RECORDER_PID=""
        adb -e pull "$CURRENT_REMOTE" "$CURRENT_LOCAL" || true
    fi
}
collect() {
    stop_recording
    adb -e exec-out screencap -p > device_artifacts/final-screen.png || true
    adb -e shell dumpsys gfxinfo com.example.onemove framestats > device_artifacts/gfxinfo.txt || true
    adb -e shell dumpsys meminfo com.example.onemove > device_artifacts/meminfo.txt || true
    adb -e pull /sdcard/Android/data/com.example.onemove/files/qa device_artifacts/phases || true
    adb -e logcat -d > device_artifacts/logcat.txt || true
}
trap collect EXIT
printf 'case\tmethod\tbudget_seconds\texit_code\tresult\n' > device_artifacts/result.tsv
FAILURES=0
run_case() {
    local name="$1" method="$2" budget="$3" recording="$4"
    CURRENT_REMOTE="/sdcard/${recording}"
    CURRENT_LOCAL="device_artifacts/${recording}"
    adb -e shell screenrecord --bit-rate 6000000 --time-limit 180 "$CURRENT_REMOTE" > "device_artifacts/cases/${name}-recorder.log" 2>&1 &
    RECORDER_PID=$!
    sleep 1
    # Give each method its own budget. The old single 170s budget cut off test 4
    # after 3 passed; this does not remove, skip or weaken any test assertions.
    set +e
    timeout --signal=TERM --kill-after=5s "${budget}s" adb -e shell am instrument -w -r \
        -e class "com.example.OneMoveDeviceJourneyTest#${method}" \
        com.example.onemove.test/androidx.test.runner.AndroidJUnitRunner | tee "device_artifacts/cases/${name}.txt"
    local status=${PIPESTATUS[0]}
    set -e
    stop_recording
    local verdict=PASS
    if [[ "$status" -ne 0 ]] || ! grep -Eq 'OK \(1 test\)' "device_artifacts/cases/${name}.txt"; then
        verdict=FAIL
    fi
    if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' "device_artifacts/cases/${name}.txt"; then
        verdict=FAIL
    fi
    if [[ ! -s "$CURRENT_LOCAL" ]]; then verdict=FAIL; fi
    printf '%s\t%s\t%s\t%s\t%s\n' "$name" "$method" "$budget" "$status" "$verdict" >> device_artifacts/result.tsv
    adb -e logcat -d -s TestRunner OneMoveQA AndroidRuntime > "device_artifacts/cases/${name}-events.txt" || true
    if [[ "$verdict" != PASS ]]; then
        FAILURES=$((FAILURES + 1))
        # Stop only our app/test process on the disposable emulator after failure.
        adb -e shell am force-stop com.example.onemove || true
        adb -e shell am force-stop com.example.onemove.test || true
    fi
}
run_case lifecycle backgroundPauseDoesNotAdvanceTheSimulation 60 lifecycle.mp4
run_case campaign all12WinningPinsCompleteCampaignThroughRealUi 170 one-move-device-journey.mp4
run_case wrong_choices causalPrototypeWrongChoicesFailForVisiblePhysicalReasonsAndRetryCleanly 90 wrong-choices.mp4
run_case retry wrongPinFailsAndRetryRestoresReadyState 45 retry.mp4
collect
trap - EXIT
COUNT=$(find device_artifacts/phases -name 'level_*_03_success.png' | wc -l)
if [[ "$COUNT" -ne 12 ]]; then FAILURES=$((FAILURES + 1)); fi
# Process name can be on the line after FATAL EXCEPTION, so inspect a short block.
python3 - <<'PY'
from pathlib import Path
import re
log = Path('device_artifacts/logcat.txt').read_text(errors='replace')
bad = bool(re.search(r'ANR in com\.example\.onemove', log))
for match in re.finditer(r'FATAL EXCEPTION', log):
    bad |= 'Process: com.example.onemove' in log[match.start():match.start()+1200]
Path('device_artifacts/crash-check.txt').write_text('FAIL\n' if bad else 'PASS\n')
PY
if grep -q FAIL device_artifacts/crash-check.txt; then FAILURES=$((FAILURES + 1)); fi
if [[ "$FAILURES" -ne 0 ]]; then
    printf 'FAIL: %s failing checks. Read result.tsv and individual instrumentation logs; no full QA approval.\n' "$FAILURES" > device_artifacts/result.txt
    exit 1
fi
printf 'PASS: four native methods, 12 visible phase completions, causal wrong choices, retry and lifecycle; four actual recordings. Review footage before visual approval.\n' > device_artifacts/result.txt
