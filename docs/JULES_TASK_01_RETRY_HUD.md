# Jules Task 01 — Repair current retry/adaptive-HUD regression

## Scope
Work only on the current Android retry/adaptive-HUD failure and the minimum code/tests needed to fix it.

Base branch: `jules/pre-tripothon`.
Read `AGENTS.md` first.

## Known evidence
The latest captured Android Lab run for head `71f1b03578d20dab4a6054d5537fe5b9b76cd798` failed in the device journey.
The observed assertion was:
`Retry READY state was not reflected by the adaptive HUD; counter='null'`.

The failing test was:
`causalPrototypeWrongChoicesFailForVisiblePhysicalReasonsAndRetryCleanly`

Do not assume the test is wrong.
Do not assume the UI is wrong.
Reproduce or inspect enough evidence to decide which one is actually wrong.

## Required investigation
1. Inspect the retry/reset flow in the ViewModel and UI.
2. Inspect the adaptive HUD semantics/test tags/state exposure.
3. Inspect the failing instrumentation test and any helper it uses.
4. Determine whether the READY state is reached but not exposed, not reached, exposed too late, or incorrectly asserted.
5. Check whether recent large-font/adaptive-HUD changes introduced the regression.

## Fix constraints
- Smallest truthful fix.
- Do not change puzzle solutions, physics constants, collision geometry or level definitions.
- Do not disable, delete, skip or loosen the failing test just to pass.
- If the assertion is genuinely stale, prove why with current UI behavior and replace it with an equally strong assertion.
- Preserve lifecycle/retry semantics.

## Verification
Run the strongest tests available in the Jules environment.
At minimum run the relevant unit/Robolectric subset.
If a real Android emulator cannot run in the Jules VM, say so explicitly and prepare the change so GitHub Android Lab can verify it.
Do not claim device validation if only JVM/Robolectric ran.

## Final response
Report:
- root cause;
- files changed;
- exact commands run;
- exact pass/fail results;
- anything not tested;
- why the fix cannot hide a real gameplay regression.

Stop after this task. Do not start audio or any other feature.
