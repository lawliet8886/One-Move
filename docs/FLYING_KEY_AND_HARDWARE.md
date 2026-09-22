# Flying Key and tactile hardware — 22 September 2026

Candidate based on73cd1d59756927b3b772c6bfec3f1d9b2718097a, branch chatgpt/android-lab, isolated Windows checkout .local-lab/session-20260922-1435. The existing original checkout, older review copies, prepared Tripo branch and all dirty parallel source/cache files remain untouched. No main write, force-push, Codex invocation or paid model call.

## Playable machinery, not a decorative extra

Level11 replaces the Across the Abyss recovery funnel with The Flying Key. Pulling A releases a heavy weight; the spring launches it across the gap into a catcher; its sustained physical contact latches a pressure switch; the linked floor-gate opens; the three friends can then reach the sanctuary. The bridge and landing floor remain real colliders. B/C/D leave the weight restrained and the lock closed. This is a combined spring/weight/switch lesson, not random timing or extra tiny buttons.

An early prototype pinned the friends outside the landing guard. It was rejected and the guide/gate geometry corrected. The accepted geometry passed180 actual JVM simulations before catalog integration: all four choices repeatedly,30/60/120Hz and irregular frame schedules,25 initial-position offsets, removal of spring/weight/switch/catcher/bridge/floor, chronological spring-latch-gate events, answer relabelling with identical positions/events and reset. The solver was not modified. Counterfactual catch trays stop a missing-key weight physically instead of relying on a timeout.

The existing campaign/input/design/counterweight/switch/return-flight regression cases remain. FlyingKeyContractCases is wired into PhysicsContractTest. A new separate native recorded case uses actual screen-coordinate touches for A/B/C/D, verifies the real loaded hardware, switch/lock state and reset; none of the prior seven cases is removed. Its110-second budget is separate from the existing wrong-choice recording. Native assertions and fresh clips must pass before claiming Android approval.

## Original offline Blender hardware

Reused tools/art/render_mascots.py's existing lighting studio to render a spherical enamel/brass weight, matte round stone and flanged brass axle. Sources are original parametric Blender meshes, NOT Tripo outputs. Three256px cells share a2.4 framing and body-centred pivot. Source scenes remain in .local-lab/hardware-pass-20260922-1440; only one108588-byte lossless WebP and a provenance manifest enter the app. Decoded atlas memory is786432 bytes. SHA256, RGBA round-trip, transparent borders and physical-diameter bounds were checked before integration; reviewed at32/48/64px as well as full resolution.

MainActivity validates and decodes this atlas once per process. Weight/stone sprites use the actual body radius, position and rotation; the axle remains on the actual seesaw pivot. Existing vector paths remain for isolated previews without Activity asset setup. No body is repositioned or resized to fit art, no root motion or answer-key lookup is added. Existing Pip/Mochi/Blobbo art remains unchanged in this pass.

The spring now reads as a three-coil metal cartridge with an enamel strike plate, brass mounting edges and a higher-contrast direction arrow derived from the actual impulse normal. Its casing is presentation; the production impulse/contact rules are unchanged. Compress/flash feedback comes from the existing physical spring state, not a separate animation clock.

## Required native review and known scope

Inspect new screenshots of levels5/6/10/11, all8 native cases, normal/compact/200%-font boards, the spring-weight flight and visible failure/reset. Compare against the exact73cd baseline, not synthetic gameplay. The existing12-level known-solution journey and17 total deliberately wrong choices are scripted regression, NOT free visual exploration. Do not call offline renders Android footage.

This adds a0.75MiB decoded texture; there is no demonstrated handset FPS improvement. Read current gfxinfo before teardown and report its software-emulator limitations. Baked lighting rotates with the 2D hardware artwork; fine material detail may simplify at phone size. The remaining four recovery-funnel boards1/4/7/12 and full character animation work are not solved by this change.

## Tripo access boundary

The CLI was authenticated in the prior turn with0 API credits; Gabriel's screenshot shows3915 Studio credits. In this turn Opera list-tabs returned Browser not connected. No Studio login bypass, cookie/key extraction, generation, purchase or top-up was attempted. The offline factory is usable; spending Studio credits still requires the actual Studio browser connection and a verified operation quote. One Move Lab automation was already paused and remains paused; no duplicate or invisible continuous execution was created.

## Actual Android review and follow-up grip correction

Native run35764885773 atc456509 passed all three jobs and all8 device methods. Downloaded and verified121 native screenshots plus8 real MP4 recordings; reviewed before/after screenshots for levels5/6 and the new level11 ready/latch/success states. Hardware and the spring were visibly integrated, with the original physical centres retained. The earlier build745159a failed the new image-loader smoke test in Robolectric's legacy graphics mode. Enabling native Android graphics for that one image test fixed it; hash/dimension/alpha assertions stayed intact.

Despite green functional tests, actual level11 screenshots showed the D grip drawn over the unrelated B bridge. The follow-up changes ONLY its handlePosition from(730,555) to(730,680), leaving every collider, body, switch, spring and goal unchanged. Its perpendicular distance to B was18.03 world units and becomes122.03, above the56-unit visual-clearance contract. A second contract requires48dp between all grip centres at the already-established minimum250dp board width. Native clicks still use actual displayed handle positions; do not regard offline spacing checks as a substitute for the next screenshot review.

Thec456509 native pass applies to that exact revision. The grip-corrected revision requires its own complete CI result and fresh Android screenshot/recording inspection before release approval. Source hardware art and physics solver remain byte-identical to the preceding candidate.
