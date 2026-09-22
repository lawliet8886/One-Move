# Return Flight and rescue UI - 22 September 2026

Candidate layered on 7cdecfa. Preserve its compact HUD, virtual-keyboard Back injection, and stronger 250x333dp visible-board assertions. This session's overlapping earlier drafts are archived locally rather than overwriting that parallel work.

Level 10 replaces another recovery funnel with a spring flight, far landing guard and downward return bridge to a left-side sanctuary. The obstruction must be removed while keeping the bridge, guard and sanctuary floor. Four choices, four timing schedules, +/-4 placement offsets, mechanism removals, relabelled solution and reset were checked in 108 actual JVM simulations using production physics and platform-value adapters. All passed. They are NOT Android gameplay. The same contract is wired into PhysicsContractTest and requires fresh CI execution.

The renderer now draws a carved, arched sanctuary entrance rather than the cushion-like destination. The goal trigger is unchanged, and real colliders/creatures render above the art. Reuses a single geometric cache, not an animation clock or new model API. Current mascot atlas remains unchanged.

Result and selector text gets explicit line heights; large-font selector uses a short heading and two content-sized columns. Normal HUD remains a reflowing presentation; 7cdecfa's compact HUD is still used for large fonts/short displays. Added three level-10 wrong choices to native recorded regression (14 total); no assertions removed.

Baseline 4ab7718 native evidence: five prior methods PASS; Back and 200%-font methods FAIL. Visual inspection showed the board had collapsed under scaled text. This failure is not approved. Corrected candidate needs fresh seven-case Android footage and screenshot review. No handset FPS claim.

Desktop Commander recovered: actual write/read/commands succeeded. Free RAM last observed below2GB; no local Gradle, render batch or emulator launch. Existing ScanFlow emulator is untouched. Main checkout e36fe48 with uncommitted parallel render work remains untouched; isolated review is used for this candidate. Both 4056946 and f0aadaf remain ancestors.

Tripo preparation is a distinct authorized branch chatgpt/tripo-asset-factory-20260922 and must not be merged without asset review and explicit cost approval. No Tripo credits spent.
