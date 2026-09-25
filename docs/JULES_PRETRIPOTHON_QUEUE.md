# Jules pre-Tripothon queue

This queue is deliberately ordered from low-risk/high-value work to more creative work.

## Task 01 — Fix the current Android retry/adaptive-HUD failure
Goal: reproduce the latest failure on commit 71f1b03 and make the smallest correct fix.
Do not weaken the test. Do not touch physics unless the evidence proves physics is the cause.
Primary acceptance:
- explain root cause;
- failing case reproduced or otherwise evidenced;
- relevant test passes after the fix;
- existing fast unit/physics suites remain green;
- no unrelated UI redesign.

## Task 02 — Make audio feedback real
Audit `SoundManager` and all call sites.
Implement only sounds that improve game-state comprehension: pin pull, impact, spring/bumper, success, failure.
Requirements:
- no network dependency;
- no unlicensed media;
- lifecycle-safe;
- no audio spam under repeated collisions;
- mute/disable path if the app already has settings, otherwise keep architecture ready for one.
Add tests for logic that can be tested off-device.

## Task 03 — Retry/failure UX pass
Choose one bounded failure/retry screen problem.
Improve clarity, touch target sizing and large-font behavior without changing puzzle geometry or outcomes.
Produce before/after evidence from Robolectric/Roborazzi or another truthful render path.
State clearly if evidence is not from a real Android device.

## Task 04 — Mascot animation validation
Audit the existing Pip/Mochi/Blobbo runtime atlas/manifest pipeline.
Add checks for missing frames, wrong dimensions, invalid hashes, clipping, inconsistent framing and impossible animation state transitions.
Do not regenerate Tripo assets.

## Task 05 — One causal puzzle prototype
Design one new puzzle using existing mechanisms before inventing new systems.
The winning path must depend on physical causes, not an answer key.
Add counterfactual tests: remove the essential mechanism and success must stop.
Do not add more than one new level in this task.

## Task 06 — Measured performance pass
Profile a clearly named bottleneck.
Make one optimization only if there is a before/after measurement.
Do not reduce visual quality silently.

## Working rule
Each task gets its own Jules session/branch or PR.
Do not run Task 02 before Task 01 is reviewed.
Do not merge any task automatically.
