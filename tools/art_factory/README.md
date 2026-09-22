# One Move offline asset factory

This branch is PREPARATION ONLY. Kotlin/Compose and the existing 2D renderer stay in place. No Unity, Godot, SceneView, Codex, model API, Tripo generation, top-up or runtime asset replacement is performed by these scripts.

## Verified billing boundary, 22 September 2026

Tripo CLI0.5.1 was installed from the official npm package in OneMove-Lab/.tools/tripo-cli-0.5.1, not globally. Node24.14.0 was already installed. Installation used --ignore-scripts --no-audit --no-fund and an isolated npm cache; no Windows policy, PATH or security setting changed.
`doctor --json --no-open` returned1: Node passed, API key missing, reachability skipped because there was no key. `balance --json --no-open` returned3: no API key configured. API balance is UNKNOWN, not zero. No login, credential search or generation was attempted.
Gabriel reports approximately4000 Studio credits expiring2026-10-01; neither balance nor expiry was independently read from Studio. The official Help Center says Studio and API balances are separate, and Studio credits cannot call the API. The CLI uses the API. Do not buy API credits or try to convert the Studio wallet through CLI/MCP.
Sources: https://developers.tripo3d.ai/en/docs/cli ; https://www.tripo3d.ai/help/api-plugins/tripo-studiotripo-api ; https://docs.tripo3d.ai/other/support-faq.html

## Tools and commands

Existing Windows Blender5.2.0 LTS was verified at C:\Program Files\Blender Foundation\Blender 5.2\blender.exe. Python3.13.7 and Pillow were already available. The new scripts reuse tools/art/render_mascots.py's original lighting and modeling helpers. They never edit that script.
From this checkout in PowerShell:
```powershell
.\tools\art_factory\OneMove-AssetFactory.ps1 -Action Preflight
.\tools\art_factory\OneMove-AssetFactory.ps1 -Action Tests
.\tools\art_factory\OneMove-AssetFactory.ps1 -Action Smoke
```
Preflight only runs doctor/balance. Tests are offline. Smoke requires at least1.5GB free RAM and2GB disk, uses one CPU thread, bounded timeouts and unique output directories. It creates a small ORIGINAL LOCAL Pip fixture, not a new Tripo asset. It never closes another app or touches an emulator.

## Files

`blender_bake.py`: local GLB/FBX inspection; explicit action binding; source FPS set before import; inherited studio lighting; orthographic yaw/pitch; transparent PNG; fixed body-centered pivot; root-motion rejection when an anchor is declared; source/recipe/frame hashes; reusable packed .blend scene.
`pack_atlas.py`: exact-size RGBA validation, nonempty alpha, clipped-silhouette rejection, hashes/path containment, duplicate rejection, bounded8MiB decoded atlas, lossless WebP round-trip, JSON manifest and labelled offline contact sheet. It never trims/recentres frames or changes collision geometry.
`smoke_factory.py`: nine atlas contracts plus actual Blender export/import/bake checks. The four GLB and FBX clip pairs must have different evaluated bone matrices; merely producing different PNG hashes is not enough. The local rig is a one-bone compatibility fixture, not finished idle/jump/fall/happy acting.
`art/asset_factory/catalog.json`: billing state, asset record template, pilot and family queue. `PIP_PILOT.md`: unsubmitted prompt, reference and ordered acceptance gates. Every future paid operation requires its own credit ledger entry and explicit authorization; all preparation spending is0.

## Studio handoff and ordered factory

Use Studio/Web for the reported expiring wallet. Its browser connector was disconnected; do not bypass login or extract cookies. The human or a future authorized browser session must verify the Studio account, wallet, expiry, exact operation quote and applicable usage rights before generating one Pip candidate. Export a self-contained textured GLB (preferred), or FBX with its textures, to a versioned source folder. Never generate one new model per animation frame.
Inspect the source before baking: vertices, transforms, action/object/bone names, materials, texture presence, orientation and body-center/radius calibration. Copy and edit the pilot recipe with explicit source names and action frame ranges; do not assume an arbitrary Tripo rig uses the fixture's names. Declare an in-place root anchor. Fix topology only for necessary deformations, then reuse one rig/model across all poses.
The next offline command after receiving a model is Blender `--background --factory-startup --disable-autoexec --threads 1 --python-exit-code 1 --python tools/art_factory/blender_bake.py -- --source PATH_TO_LOCAL_GLB --out NEW_INSPECTION_DIRECTORY --inspect-only`. No Tripo call occurs. The wrapper's Preflight command is safe before any spending; authenticating the CLI would only reveal the separate API wallet, not unlock Studio credits.

## Android integration gate, not completed here

The new manifest is NOT a drop-in replacement for the current fixed4x3 atlas. Add a bounded, opt-in atlas loader in a later reviewed candidate, retaining the existing mascot atlas as fallback. Load once, validate hashes and dimensions, and select per-character clips from actual physical velocity/grounded/inside-goal states. Never read solutionPinId or move a physical body from animation data. Use sprite_side = actual_radius * manifest.framing and one shared center. Visible art must not pretend a different collision radius.
An idle loop must not advance physics while READY or in the background. Do not reintroduce HUD recomposition for every frame. Respect lifecycle/reduced-motion choices where implemented; benchmark any new animation work before claiming it is faster or smoother.
Run the independent physics/answer-label contracts unchanged, native correct/wrong choices, retry, rapid taps, phase changes, HOME/return, Back, compact and200%-font checks. Capture a real Android screenshot/video tagged with the exact integration commit. Compare readability at32/48px, HUD portraits, all frame pivots, alpha fringes, mechanics, memory and controlled frame timings. Only then mark approved_for_runtime=true and integrate the chosen candidate into the authorized Android branch. No Android art approval is asserted by offline images.

## Risks and scope

Studio credits are not API credits; missing authentication is not zero balance. Generic round mascots may need custom facial/appendage rigging. Procedural Blender materials do not necessarily survive GLB/FBX export as textures; bake or verify them explicitly. FBX action names/axes can change. Root motion, per-frame cropping and independent auto-scaling can visually lie about physics. Low-sample smoke renders are intentionally noisy; they are not final material quality. Keep source models, texture files, scripts, recipes and hash records together. Expiry pressure is not a reason to generate random batches.

## Parallel Android state

The separate Android checkout remains on7cdecfa. Run35751321023 attempt2 passed build/physics and five of seven native methods, including real Back and200%-font gameplay; measured large-font board289x385.5dp. Wrong-choice retry at level9 and the compact counter text assertion failed. An attempted next layout-correction script write was blocked by the platform and was NOT executed or rerouted. These issues remain open. The prep branch does not claim to fix them, modify main or re-enable the paused One Move Lab automation.
