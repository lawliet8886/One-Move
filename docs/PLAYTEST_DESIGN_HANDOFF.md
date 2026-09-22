# One Move — design improvement handoff

Baseline: 3b0027280a80ff854c01b706122b8d30a219bb68, chatgpt/android-lab.

## User feedback and actual goal

Gabriel reviewed the native recording: the campaign is too easy, repetitive and visually simple. A green build and a known-solution replay are not evidence that the game is fun. Prioritize observable decisions, causal machinery, fair feedback and an intentional art direction. Keep main untouched. Do not use Codex, paid model APIs or Tripo credits without separate authorization.

## This change

Only levels 2, 5 and 8 are replaced by causal puzzle prototypes: keep a bridge while removing its obstruction; use a spring to cross a gap while preserving a landing; keep a shield and bridge while opening an exit under a heavy weight. The production physics engine is unchanged. The spring graphic is normalized to the same direction as its impulse and receives a visible launch chevron.

Offline Kotlin/JVM validation executed before commit: 135 added design simulations across every choice, repeated 30/60/120 Hz and irregular schedules, answer-key relabelling, ±2 world-unit placement perturbations and mechanism-removal counterfactuals. All passed. An additional 37-choice campaign smoke pass succeeded without simulation timeouts. These runs use lightweight platform value adapters, not Android or Compose rendering.

Native compilation, full existing contracts, device touches and fresh video MUST be evaluated on this commit before accepting the prototypes. Never reuse the baseline video as evidence for this revision. Existing known-solution native journeys are not blind visual playtests. Do not weaken failing tests to accept defects.

## Remaining work

Nine boards still use the recovery funnel. These three prototypes do not establish a final difficulty curve. Reject decorative machinery: removing a purportedly essential mechanism should change a result for a physical reason. Vary decisions and cause/effect chains, not simply the number or colors of pins. Some blocked wrong choices still need more interesting feedback. Test wrong choices on-device as well as winning paths; extend small-screen/font and performance evidence. Keep the original independent anti-teleportation contracts.

Art direction proposal: tactile miniature rescue machines and soft collectible creatures, with readable foreground hazards and warm material contrast. Evaluate 2.5D pre-rendered assets before replacing native physics/rendering with a full 3D engine. Tripo Studio and API balances are separate; connecting a Tripo API MCP does not automatically use Studio credits.

Remote Desktop Commander was surfaced as an installable ChatGPT app but is not connected. A local device, emulator and authenticated bridge would allow an observe–tap–observe trial; do not claim these hands exist until the actual calls succeed. Approved folders are not OS isolation.

## Coordination

Read the branch head and CI before editing. Preserve newer commits and use fast-forward-only updates. Let an active run finish instead of continuously canceling it. Small reversible changes, fresh evidence, truthful reporting; no perfection guarantees or infinite autonomous loops.
