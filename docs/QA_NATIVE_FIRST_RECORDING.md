# First native Android recording and resulting fixes

Source run: 35711747144, commit 0ee1fc693bef3ac162eb281c6fa3c503c300b7fd.

## Executed
- App and instrumentation APKs compiled; smoke, campaign matrix and independent physics/input contracts passed.
- Actual Android emulator executed and recorded all twelve winning screen-coordinate journeys. The instrumentation marked the complete campaign and wrong-pin/retry tests successful.
- 52 native screenshots, a real 169.87-second MP4, and journey.tsv with twelve SUCCESS rows were retrieved and inspected.
- The third background/resume test never completed before the 170-second harness timeout. The overall device job FAILED; do not report that run as all green.

## Defects found in native evidence/code
- Screenshots captured a failure card over a new level showing 1 MOVE LEFT: the previous success AnimatedVisibility exit was re-rendering with the new READY state. Dismiss result and selection overlays synchronously to remove the stale-frame flash.
- The frame coroutine requested display frames even while READY or long after particles ended. Gate frame requests on actual visible work. The ViewModel also ignores terminal idle ticks.
- Pausing through ActivityScenario while animation is active can wait for an idle boundary. The new test uses real HOME input, verifies the game is RUNNING before measuring frozen time, and resumes the same task. This must be re-run; do not assume the timeout alone proved a game lifecycle defect.
- Local object rotations were using the Canvas centre instead of Offset.Zero after translation. Anchor seesaws, weights and confetti to their own physical centres; match heavy-body degree units.
- Dark status-bar icons were illegible on the dark game. Explicitly use dark SystemBarStyle (light icons) and the dark application theme.

## Still required
Native repeat on this revision, screenshot review for stale overlays and aligned materials, small-screen and font-scale coverage, more distinctive puzzle mechanics and independent live exploration. This is not a final quality or performance certification. Main remains untouched.
