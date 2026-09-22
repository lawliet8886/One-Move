# Windows lab preparation

Work only on `chatgpt/android-lab`; the helper refuses other branches.
No API keys, paid model calls, Codex, automatic downloads or global Windows changes.
The project directory is a dedicated workspace, NOT an operating-system sandbox.

## Available commands (PowerShell, from the project root)

```powershell
.\tools\windows\OneMove-Lab.ps1 -Action Doctor
.\tools\windows\OneMove-Lab.ps1 -Action Build
.\tools\windows\OneMove-Lab.ps1 -Action Test
.\tools\windows\OneMove-Lab.ps1 -Action PrepareDevice
.\tools\windows\OneMove-Lab.ps1 -Action StartDevice
```

SDK defaults to `%LOCALAPPDATA%\Android\Sdk`; Java is resolved from PATH.
Build uses Gradle 9.3.1 and the existing Android SDK; tests retain independent physics contracts.
A dedicated API 34 AVD reuses an installed Google APIs image and does not overwrite other AVDs.
Local files, logs and emulator data stay under `.local-lab/`; `local.properties` stays untracked.
RAM guards require 3 GB free for build, 5 GB for tests and 4 GB for emulator start.
These are conservative project guards, not official Android hardware requirements.
No program is closed, no user device is wiped and no security setting is changed.
Wrapper generation, official JAR checksum and helper parsing/Doctor/resource guard were verified.
This setup change is NOT evidence of a local Android build, a new gameplay video or visual polish.
