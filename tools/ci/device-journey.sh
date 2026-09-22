#!/usr/bin/env bash
# Real Android evidence. Run only against the disposable emulator created by CI.
set -euo pipefail
cd "$(dirname "$0")/../.."
mkdir -p device_artifacts
printf 'Automated screen-coordinate Android journeys; not unscripted human play.\n' > device_artifacts/README.txt
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
collect() {
    set +e
    adb -e exec-out screencap -p > device_artifacts/final-screen.png
    adb -e shell pkill -INT screenrecord
    if [[ -n "$RECORDER_PID" ]]; then wait "$RECORDER_PID"; fi
    adb -e pull /sdcard/one-move-device-journey.mp4 device_artifacts/one-move-device-journey.mp4
    adb -e pull /sdcard/Android/data/com.example.onemove/files/qa device_artifacts/phases
    adb -e logcat -d > device_artifacts/logcat.txt
}
trap collect EXIT
adb -e shell screenrecord --bit-rate 6000000 --time-limit 180 /sdcard/one-move-device-journey.mp4 > device_artifacts/recorder.log 2>&1 &
RECORDER_PID=$!
sleep 1
set +e
timeout 170s adb -e shell am instrument -w -r -e class com.example.OneMoveDeviceJourneyTest \
    com.example.onemove.test/androidx.test.runner.AndroidJUnitRunner | tee device_artifacts/instrumentation.txt
STATUS=${PIPESTATUS[0]}
set -e
# am instrument can exit 0 even when JUnit failed. Require an explicit success summary.
if [[ "$STATUS" -ne 0 ]] || ! grep -Eq 'OK \([0-9]+ tests?\)' device_artifacts/instrumentation.txt; then
    exit 1
fi
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' device_artifacts/instrumentation.txt; then
    exit 1
fi
collect
trap - EXIT
set -e
test -s device_artifacts/one-move-device-journey.mp4
COUNT=$(find device_artifacts/phases -name 'level_*_03_success.png' | wc -l)
test "$COUNT" -eq 12
printf 'PASS: 12 visible phase completions and native recording captured. Review footage before visual sign-off.\n' > device_artifacts/result.txt
