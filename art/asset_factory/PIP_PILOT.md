# Pip: one model, several usable performances

Status: preparation only. Do not submit generation, texture, retopology, rig or animation requests yet.
Canonical reference: references/pip_current.png, copied from the current game's original atlas. Review against the live Android screenshots before accepting any replacement.

## Proposed Studio prompt, NOT submitted

A single original small round rescue-toy fox named Pip, matching the supplied reference: compact orange plush body with a cream face bib, short triangular ears with rose lining, large readable dark eyes, a tiny dark button nose, small inset paws and little arms. Warm tactile felt and satin details, friendly miniature workshop toy, simple strong silhouette readable at 32 pixels. Keep the same proportions and identity in every view. Neutral symmetrical pose, mouth separated and suitable for later facial posing, limbs and ears not fused into the torso. No pedestal, scene, text, watermark, extra limbs, accessories, realistic fur strands or large tail obscuring the body. One reusable textured model, not a sheet of characters.

## Ordered gates

Reference -> Studio wallet and displayed operation quote -> explicit spending release -> one generation -> inspect front/back/side/silhouette -> choose one candidate -> repair topology only where deformation requires it -> verify textures/materials -> one reusable rig -> in-place idle/jump/fall/happy -> bake -> atlas tests -> isolated Android integration -> actual emulator capture -> before/after review.
Do not run texture/retopo/rig again automatically after a failure; each may consume credits. Record each operation separately, including quoted cost, actual wallet delta, status and source model version. Download accepted 3D files and derived materials before the reported expiry; retaining files is not proof of any particular license.

## Animation contract

Proposed first production set: idle8, jump6, fall4, happy8 frames at12fps,192px cells. This is a proposal, not generated content. All clips share one camera, lighting, body center, framing and alpha padding. Real jump/fall positions remain exclusively in PhysicsWorld. Strip root translation before baking; the current bake guard rejects moving root anchors rather than compensating in gameplay. A root-only test rig is not final acting. Facial expressions and appendage poses need visual review and may require custom Blender work when automatic rigging does not fit Pip's round anatomy.

## Art and mechanics

Compare at32px and48px body size, then at the larger HUD portrait size. Inspect eyes, ear tips, transparent fringes, feet grounding and continuity across clips. Do not enlarge the body beyond its physical radius to make it look more impressive. Use the atlas manifest framing to map physical radius to sprite size; do not independently recenter or trim frames.
Expand to Mochi and Blobbo only after Pip passes. Use parametric Blender parts for exact pins, rails, hinges and coils instead of paying Tripo to approximate mechanical geometry. Use generated models where they add character, not where they introduce collision ambiguity.
