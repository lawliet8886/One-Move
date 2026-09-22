#!/usr/bin/env bash
# Run only against the dedicated CI emulator. Always collect evidence on failures.
set -euo pipefail
mkdir -p lab-evidence/device lab-evidence/videos
collect() {
  adb logcat -d -v threadtime > lab-evidence/device/logcat.txt 2>&1 || true
  adb pull /sdcard/Android/data/com.example.onemove/files/lab/. lab-evidence/device/ || true
  adb pull /sdcard/Download/one-move-lab/. lab-evidence/videos/ || true
  adb exec-out screencap -p > lab-evidence/device/last-screen.png || true
}
trap collect EXIT
adb shell wm size 1080x1920
adb shell wm density 320
adb shell settings put system show_touches 1
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1
adb shell settings put global animator_duration_scale 1
set -o pipefail
./gradlew --no-daemon --console=plain --stacktrace :app:connectedDebugAndroidTest 2>&1 | tee lab-evidence/instrumentation.log
