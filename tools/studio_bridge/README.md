# One Move Studio bridge — actual local MCP, not an official Tripo API

Runtime: Python3.13, Playwright1.61.0, official MCP SDK2.2.0. Dedicated Edge/CDP remains at127.0.0.1:9222, with its own profile, synchronization/extensions disabled. Only the explicitly pinned Studio tab is used; other Studio/personal tabs are preserved. API wallet is not used.

## Commands

From this directory:
`python bridge.py status` — visible wallet/login/controls in one DOM pass.
`python bridge.py quote` — exact Pip preview hash, displayed cost and settings; never generates.
`python bridge.py snapshot` — actual Studio screenshot, labelled separately from Android.
`python bridge.py inspect` — bounded visible panel text.
`python bridge.py submit --operation-id APPROVED_ID` — one native keyboard activation only after a local, one-use approval is validated and durably reserved. No automatic generation retries.

The local MCP server (`mcp_server.py`) exposes these operations plus `studio_operation` and `studio_download_existing_glb`. It uses STDIO, not a public HTTP listener. A compatible MCP client can run the server with the project-local environment. No Codex process was launched or configured. ChatGPT currently invokes the same adapter through Desktop Commander; this server was not secretly installed as a ChatGPT connector.

## Safety contract

Local state lives in OneMove-Lab/.local-lab/tripo-bridge-state, outside Git. SQLite reserves an operation before dispatch. A repeated operation id returns its prior record instead of clicking. Unknown/changed price, wallet, reference, quality, topology, stale quote and missing/expired approval reject. Any ambiguous post-dispatch failure blocks further automatic spending until reconciled. Approvals are not editable through an MCP tool. No top-up, API task, cookie database, storage export, password/token lookup, arbitrary browser evaluation or terminal tool is exposed.

A browser automation client is still less robust than an official service API. The page structure can change; authentication/CAPTCHA requires the user. Disconnection is not permission to force restart browsers or choose a different tab. Generated models are not automatically approved or installed in the game.

## Verified pilot, 22 September 2026

One Pip generation consumed55 Studio credits:3915->3860. Model page id858cc001-0b10-4353-844c-7d5fde59fdc9; v3.1 maximum quality. Exported current4K-textured GLB58,644,720 bytes, SHA2563305e28f497983b30ab71f75eb707f843358ad269be72af7008b8a8f5f47951e. Export did not change the balance. Source has1,884,560 triangles,962,888 vertices, one material, three textures and NO animations.

The earlier mouse action timed out BEFORE input dispatch. Its ledger record remains failed with zero spend. After checking the trace, unchanged wallet and form, one Enter activation created the model. Subsequent same-id MCP submission was verified not to re-click. Never repeat a paid operation merely because a screenshot or response timed out.

## Test evidence and limitations

`python test_guard.py` passed13 offline spending/idempotency tests. `test_mcp_live.py` used the official MCP Client and a real STDIO server process: tool discovery, three live status calls, repeat-paid-id rejection and missing-approval rejection passed without spending. Latest warm status round trips87.17/98.09ms; first connection4831.6ms. These are measured calls on this computer, not a guaranteed latency or a controlled before/after benchmark.

`prepare_pip.py` reuses the existing Blender lighting studio. The original GLB is preserved; an offline derivative reduced1,884,560 faces to65,000 and produced front/three-quarter/back inspection PNGs and an editable .blend. No additional Tripo credit was used. The source has no rig/animations; the new Pip is NOT ready to replace the current animated-expression atlas. Low-sample previews are noisy and are inspection images, not final art or Android footage.

Next asset gate: inspect the reduced silhouette and material at32/48/64px, correct facial depth and appendages if needed, build an in-place rig/expressions on one reusable model, bake under the same calibrated light/camera, validate atlas pivots and actual Android comparison before any runtime approval. Never upload a58MB raw GLB into the2D Android app.

Local deliverables: raw source and source-manifest under `.local-lab/tripo-bridge-state/assets/pip-20260922-55-v2`; offline views/.blend under `.local-lab/pip-studio-review-20260922-1710`. State, profile, approvals, wallet history and raw3D files are not versioned by this adapter.

References: official Python SDK https://github.com/modelcontextprotocol/python-sdk ; Playwright CDP https://playwright.dev/python/docs/api/class-browsertype#browser-type-connect-over-cdp ; browser download events https://chromedevtools.github.io/devtools-protocol/tot/Browser/ . No private Tripo endpoints are called.
