# Draw-phase invalidation, not whole-screen state every frame

Candidate layered on e36fe48, whose build, physics and five native methods passed in run35744860514. The production physics solver and geometry are unchanged by this optimization.

Previously every onFrame incremented GameUiState.frameTick and published it through StateFlow, invalidating the full screen/HUD composition even when only body positions changed. The candidate keeps the high-frequency tick in a separate LongState read only by the Canvas draw lambda. Semantic UI state changes only for relevant events; HUD creature portraits receive copies, not mutable references to the physics bodies.

An explicit needsAnimation flag keeps the loop alive through running physics, terminal presentation delays and remaining effects, then stops it. Reset and level load invalidate drawing separately. Lifecycle RESUMED gating, real HOME test and level-select pausing remain intact. Legacy frameTick remains for isolated visual preview tests, but is not advanced by the runtime game loop.

Two added Robolectric tests are included in the existing smoke-test class: 120 active frames must keep drawing while publishing fewer than20 HUD states; terminal effects must quiesce, show the result, report three rescues, and reset to zero while invalidating the board. They must execute in CI; source inspection alone is not validation.

References read2026-09-22: https://developer.android.com/develop/ui/compose/phases and https://developer.android.com/develop/ui/compose/performance. Defer rapidly changing state reads into the phase that needs them.

Compare fresh native captures and five recorded methods with e36fe48. Cloud-emulator debug gfxinfo is diagnostic only, not a physical-phone FPS benchmark. No claimed speedup until measured; preserve failures and test budgets.

Desktop Commander writes worked earlier in this session, but later file access and ping timed out. Equivalent reviewed ViewModel/screen/test changes are also present uncommitted in C:\Users\biel_\OneMove-Lab. On reconnect, compare before reconciling; never reset or overwrite newer local work. Do not change permissions or reconfigure the device to bypass this connection outage.
