# Static backdrop caching — measured concern, not an FPS claim

Native run35735395603 at8fc91f passed all four functional methods and loaded the actual12-expression atlas. However, captured campaign gfxinfo reported679 frames and100% janky frames, with250ms median in the software-rendered, instrumented cloud emulator. This is a serious performance signal. It is not a controlled physical-phone benchmark and cannot be translated directly to handset FPS.

A narrow candidate optimization rasterizes only static background and decorative hazards once per phase/geometry/density/layout into one1200x1600 RGBA image (7,680,000 bytes). The cache retains only one active bitmap. All characters, mechanisms, pins, goals, rail shapes, particles and the actual physics remain live. Avoid manual recycle of a bitmap still potentially used by the rendering thread.

Compare fresh real Android screenshots with8fc91f for graphical regressions. Inspect OneMoveRender generation logs and gfxinfo captured before teardown. Verify background regeneration on phase and compact density/font changes. Do not claim higher FPS from source inspection or from the video frame rate metadata. A software-emulator comparison alone is not a phone performance certification.

The same5 native tests remain mandatory, including smaller screen/large font. This candidate has not been natively validated at authoring time. Main untouched; no model API or external asset credit spent.
