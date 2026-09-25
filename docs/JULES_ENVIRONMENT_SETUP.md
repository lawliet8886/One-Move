# Jules environment setup

Use the Jules web UI for this repository so the starting branch can be selected explicitly.

Repository: `lawliet8886/One-Move`
Starting branch: `jules/pre-tripothon`

## Initial Setup script
Paste this into Jules > Codebase > Configuration > Initial Setup, then run **Run and Snapshot**:

```bash
set -e
set +x
. /opt/environment_summary.sh || true
set -x

java -version
git --version
chmod +x ./gradlew
./gradlew --version

echo "ANDROID_HOME=${ANDROID_HOME:-<unset>}"
echo "ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-<unset>}"
if command -v sdkmanager >/dev/null 2>&1; then
  sdkmanager --version || true
fi
```

This setup deliberately does not install a machine-specific Android SDK into the repository.
The first Jules task should inspect the VM and install/configure SDK tooling in the task environment if needed.

## Why use the web UI
Current Jules Tools CLI supports repository selection but not a native branch flag.
For this project, selecting `jules/pre-tripothon` explicitly in the web UI avoids accidentally starting from the older default branch.

## First task
Use the prompt in `docs/JULES_TASK_01_RETRY_HUD.md`.
Do not start the audio task until Task 01 has been reviewed.
