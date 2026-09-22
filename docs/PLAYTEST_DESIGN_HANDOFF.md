# One Move — design improvement handoff

Baseline: 3b0027280a80ff854c01b706122b8d30a219bb68, chatgpt/android-lab.

## User feedback and actual goal

Gabriel reviewed the native recording: the campaign is too easy, repetitive and visually simple. A green build and a known-solution replay are not evidence that the game is fun. Prioritize observable decisions, causal machinery, fair feedback and an intentional art direction. Keep main untouched. Do not use Codex, paid model APIs or Tripo credits without separate authorization.

## Causal puzzle work

Levels 2, 5 and 8 are causal prototypes: keep a bridge while removing its obstruction; use a spring to cross a gap while preserving a landing; keep a shield and bridge while opening an exit under a heavy weight. The production physics engine remains genuine fixed-step collision/gravity. solutionPinId is QA documentation only and must never drive motion or outcomes.

Offline Kotlin/JVM validation on the prototypes covers every choice, repeated 30/60/120 Hz and irregular schedules, answer-key relabelling, ±2 world-unit placement perturbations and mechanism-removal counterfactuals. These are physics/design checks, not Android gameplay or a fun certification.

## Native run 18 finding (commit 695d96d)

Build-smoke and physics-regression passed. The new real-Android wrong-choice journey also passed all five prototype failures and retry resets, and the known-solution journey visibly completed all 12 levels. The job failed only in the lifecycle test: the short level-1 winning drop had already reached SUCCESS before HOME finished backgrounding the Activity. This is a test-fixture timing regression, not evidence that background physics advanced. Use the longer level-5 spring route as the lifecycle fixture and keep the strict RUNNING/pause assertions.

The failed run still captured a fresh native video plus 67 QA screenshots. Visual review of the 12 READY states confirms the user's criticism: the logical prototypes are better, but the campaign still reads as thin lines on a very dark repeated board. The goal is too sofa-like at a glance, rail contrast is weak, and the chassis does not yet create the miniature-machine depth promised by the art direction.

## Visual direction now being implemented

Move toward a tactile rescue-machine cabinet without changing collision truth: chapter-specific cabinet moods, recessed enamel play surface, edge conduits, stronger joint collars, brighter satin rails, a clearly glowing brass sanctuary portal, and slightly stronger plush-creature separation. Decorations stay outside the causal puzzle geometry. Do not use decorative arrows or fake trails that reveal the answer.

Reference principle: compact one-touch mechanical puzzle machines can feel premium when controls, route geometry and materials read immediately at phone scale. Use that principle, not another game's exact layouts or assets. Tripo Studio and Tripo API have separate credit systems; do not attempt to spend Gabriel's Studio credits through an API/MCP route. No Tripo credits have been used.

## Remaining campaign work

Nine boards still use the recovery funnel. These three prototypes do not establish a final difficulty curve. Reject decorative machinery: removing a purportedly essential mechanism should change a result for a physical reason. Vary decisions and cause/effect chains, not simply the number or colors of pins. Some blocked wrong choices still need more interesting feedback. Test wrong choices on-device as well as winning paths; extend small-screen/font and measured performance evidence. Keep the independent anti-teleportation contracts.

Suggested progression: tutorial drop -> keep/remove structural reasoning -> linked gate -> spring trajectory -> weighted pivot -> heavy-object protection -> combined mechanisms. Late levels should combine learned concepts without becoming timing lotteries.

## Desktop lab

A Windows Desktop Commander connection now exists in the interactive workstream and a local OneMove-Lab checkout was prepared there. Treat it as a convenience, not proof of Android availability in a future run. Never close unrelated user applications merely to free resources. Prefer GitHub CI when local memory is constrained.

## Coordination

Read the branch head and CI before editing. Preserve newer commits and use fast-forward-only updates. Let an active run finish instead of continuously canceling it. Small reversible changes, fresh evidence, truthful reporting; no perfection guarantees or infinite autonomous loops.
