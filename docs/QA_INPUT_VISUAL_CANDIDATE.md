# Input, lifecycle and presentation candidate

## Implemented
- Resolve overlapping pin targets by distance, with handle priority and stable tie-breaking. Minimum handle diameter of 48dp in the adaptive board, rejected non-finite input.
- Display-synchronised frame clock that pauses below RESUMED and while selecting a level. Reset cancels delayed result presentation. No background 16ms polling job.
- Two-row readable HUD, bounded board aspect-fit, rescue counter, human-readable failure messages, final-campaign screen and accessible pin actions.
- Lit mascot bodies matching collision radii, distinctive faces, metallic labelled pin handles and visible pin withdrawal.
- Nest rendering respects physical positions. Removed the imported presentation-only docking/teleportation logic.
- Native UiAutomator screen-coordinate campaign, retry and background/resume checks with required screenshots. Dedicated shell script preserves recorder state and checks actual instrumentation success.

## Executed evidence
The unchanged production physics plus hit-testing ran locally under a non-Android JVM: 598 simulations and 6,962 checks. Reproduce with `bash tools/headless/check.sh` using Kotlin/Java. The adapters replace only Compose value types and vibration; they do not implement Android UI or rendering.

Physics commit 91a120491424cf267d737fbdfefc9d4f829b41f5 compiled a debug APK in GitHub Actions; smoke and campaign physics jobs passed. The emulator booted, but the prior Android test failed to compile because fetchSemanticsNode/fetchSemanticsNodes were incorrectly imported as top-level functions. No device gameplay video resulted from that run.

This commit replaces that device test with UiAutomator and compiles the test APK before starting the emulator. The new UI/visual changes still require native compile and screenshot review. Do not treat this document as an Android pass report.

## Remaining
Actual 12-level video review, small-screen/large-font native checks, independent puzzle-design improvements, art consistency and measured performance. Keep main untouched. Do not call scripted test input unscripted human play or present imported screenshots as new evidence.
