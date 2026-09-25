# AGENTS.md — Jules pre-Tripothon work

## Mission
Use Jules to improve One Move before the Tripothon strategy is handed to Codex.
Work in small, reviewable tasks. Do not redesign the whole game in one task.

## Canonical branch and safety
- This branch starts from `chatgpt/android-lab` commit `71f1b03578d20dab4a6054d5537fe5b9b76cd798`.
- Never rewrite or force-push `main` or `chatgpt/android-lab`.
- Prefer minimal changes and preserve existing physics behavior unless a task explicitly targets physics.
- Do not delete or weaken tests just to make CI green.
- Do not change expected outcomes to match a bug.
- Do not use paid APIs, spend Tripo credits, or add secrets unless the user explicitly authorizes it.
- Do not claim a change is verified unless the named command actually ran and passed.

## Current known state
- Android/Kotlin/Jetpack Compose app, Java 21, compileSdk/targetSdk 36.
- The latest captured Android Lab run for commit 71f1b03 failed in device-journey.
- The observed failure was in the wrong-choice/retry journey: the adaptive HUD counter was null after retry.
- Build-smoke and physics-regression are separate jobs; preserve their coverage.
- The project has deterministic physics tests, Robolectric/Roborazzi visual tests, Android instrumentation, and recorded-device evidence.
- Passing tests are engineering evidence, not proof that the game is fun.

## Work order
1. Reproduce/diagnose the current retry/adaptive-HUD CI failure and make the smallest truthful fix.
2. Strengthen or implement useful audio feedback without touching puzzle outcomes.
3. Improve one bounded UI/retry/accessibility issue at a time with visual evidence.
4. Strengthen mascot/animation asset validation and transitions.
5. Prototype at most one new causal puzzle at a time, with counterfactual tests.
6. Optimize only with before/after measurements.

## Review contract
Every completed task must report:
- exact files changed;
- exact commands/tests run;
- pass/fail results;
- what was not tested;
- any behavior intentionally left unchanged;
- screenshots or generated visual evidence when the task is visual.

## Tripo boundary
Existing Tripo-derived assets may be inspected and reused when already present in the repository.
Do not trigger generation, API calls, purchases, or credit spending.
Do not invent a new Tripo integration unless the task explicitly asks for it.
