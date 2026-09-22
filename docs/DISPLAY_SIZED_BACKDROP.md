# Display-sized background raster — candidate on 68e868d

The previous cache always allocated a 1200x1600 RGBA raster (7,680,000 pixel bytes), even when the actual Android board was only 578x771px. The candidate uses the displayed Canvas width, preserves the 3:4 world aspect ratio, caps at the authored world size and redraws to world coordinates. The key includes size/density/font/layout direction/level/hazards. Only one raster is retained; no manual recycle races.

Only paper, foliage and danger decoration are cached. Actors, physical rails, pins, controls, results, particles and machinery remain live. No game level, solver, answer key, spawn, trigger, input target or animation clock is modified. The exact same production PhysicsWorld file is retained.

New native-graphics Robolectric contracts compare cached pixels with direct background rendering at 250,578,1056px widths; bound mean RGBA channel error below2/255; verify exact allocated raster bytes; require unchanged scenes to reuse their cache and level/hazard changes to invalidate it. Images are native graphics offscreen renders, NOT Android gameplay. The pre-existing 200%-font device case additionally records actual cached bytes and requires less than2MB while retaining its full gameplay/input/accessibility assertions.

Source-level expectation at578x771:1,782,552 bytes instead of7,680,000 for THIS background raster only. This is not whole-app RAM and not an FPS gain. Wait for fresh tests and recordings before reporting it as measured. Preserve cloud debug/software-emulator caveats; no physical-phone performance claim.

Tripo work is independent on chatgpt/tripo-asset-prep. The first Studio Pip cost55 credits, wallet3915->3860, and was exported as a local58.6MB GLB. It is not copied into this APK. Offline preview reduction to65k faces and visual/animation suitability need separate approval.
