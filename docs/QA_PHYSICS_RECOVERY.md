# Physics recovery — 22 September 2026

## Verified defect in the imported baseline

The imported PhysicsWorld compared chosenPinId with solutionPinId. Correct choices interpolated creatures into the nest; other choices forced a timed defeat. No collision solver ran. Most catalog entries reused a stacked-pin layout with empty mechanism lists. Earlier green outcome matrices therefore did not prove physical causality.

## Candidate implementation

Replace scripted outcomes with a fixed 1/240 s circle/segment solver, gravity, solid supports, circle/circle contact, linked gates, weighted pivots, spring contact and actual hazard/goal detection. Victory requires all creatures physically inside the nest and settled. Wrong paths, hazards and simulation timeout are reported separately. Effects continue after terminal states. Retune all 12 boards to provide traversable, collision-tested routes. This is a recovery campaign, not final puzzle-difficulty or visual sign-off.

## Evidence already executed outside Android

598 headless JVM simulations, 6,947 checks: five frame-time schedules, repeated campaign choices, answer-key relabelling, moved goals, a sealing barrier, thin-wall high-speed inputs, reset, invalid input, actual spring bounce and post-victory particles. The production Kotlin physics/model sources were compiled unchanged with value-only Color/Rect/Offset adapters and no-op vibration. No Android runtime, APK, UI or video was exercised by that run.

The same PhysicsContractCases are invoked by PhysicsContractTest on the normal project test classpath. A successful CI run is still required before Android compatibility can be claimed.

## Remaining acceptance gates

- APK and standard project tests on the current commit.
- Android touch-based campaign and retries, lifecycle, small screens and readable UI.
- Actual recorded gameplay for all 12 levels, identified by commit; never a slideshow or synthetic simulation labelled as Android footage.
- Independent visual review and meaningful difficulty/mechanism design; do not declare perfection from a green test count.
- Keep main unchanged until the user reviews the candidate.
