#!/usr/bin/env bash
# Dedicated emulator only. Preserve private debug-app evidence even if a test fails.
set -euo pipefail
mkdir -p lab-evidence/device lab-evidence/videos lab-evidence/recorder
recorder_pid=''
collect() {
  if [[ -n "$recorder_pid" ]]; then kill "$recorder_pid" 2>/dev/null || true; wait "$recorder_pid" 2>/dev/null || true; fi
  adb logcat -d -v threadtime > lab-evidence/device/logcat.txt 2>&1 || true
  adb shell dumpsys gfxinfo com.example.onemove framestats > lab-evidence/device/gfxinfo.txt 2>&1 || true
  adb exec-out run-as com.example.onemove tar -C files/lab -cf - . > lab-evidence/device-evidence.tar || true
  tar -xf lab-evidence/device-evidence.tar -C lab-evidence/device/ || true
  adb pull /sdcard/Download/one-move-lab/. lab-evidence/videos/ || true
  adb exec-out screencap -p > lab-evidence/device/last-screen.png || true
}
trap collect EXIT
adb shell getprop ro.build.version.sdk > lab-evidence/device/android-api.txt
adb shell getprop ro.build.version.release > lab-evidence/device/android-release.txt
adb shell wm size 1080x1920
adb shell wm density 320
adb shell settings put system show_touches 1
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1
adb shell settings put global animator_duration_scale 1
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
python3 tools/record-device.py &
recorder_pid=$!
adb shell am instrument -w -e class com.example.CompactUiTest,com.example.DevicePlaythroughTest com.example.onemove.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tee lab-evidence/instrumentation.log
# adb exit status alone does not reliably encode assertion failures.
grep -Eq 'OK \(2 tests\)' lab-evidence/instrumentation.log
! grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed' lab-evidence/instrumentation.log
# Seeded random touch/motion stress, restricted to this app; not an intelligent player.
timeout 90s adb shell monkey -p com.example.onemove --pct-touch 75 --pct-motion 25 --throttle 50 -s 20260922 -v 300 2>&1 | tee lab-evidence/monkey.log
grep -Eq 'Events injected: 300' lab-evidence/monkey.log
! grep -Eq '// CRASH:|// NOT RESPONDING:|Monkey aborted' lab-evidence/monkey.log
