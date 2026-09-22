# Playfield first at large font sizes

Baseline: 4ab77180dee47c72d565acb1573b0f487328828a, native run35748448898.
Inspected actual screenshots large_font_00_ready, level_09_00_ready and compact_02_success.
At640x1136,320dpi,font2.0, the flexible HUD and multiline footer consumed almost all height; the board was effectively a point. Visible reset/menu controls alone did not establish playability.

This candidate keeps the existing artwork and normal HUD. At short heights or fontScale>=1.6 it uses a compact header with48dp controls, one-line full-semantic level title, move count and rescue count. It removes secondary chapter/footer/portrait chrome rather than overriding fontScale. The board still preserves its authored aspect ratio and coordinate transform. Physics and level answers are untouched.

Strengthen native200%-font QA to require at least250x333dp of visible playfield before any pin action and again after next-level navigation. Existing wrong choice, five rapid taps, retry, success and next assertions remain. Record actual board bounds in the artifact.

The baseline also stopped both new tests on UiDevice.pressBack() returning false. That helper combines key delivery with a particular accessibility event. Replace it with explicit real KEYCODE_BACK down/up injection, asserting each result, then retain the original sheet-dismissal, resumed-Activity and unchanged-physics assertions. This is not a direct ViewModel call or removal of the Back test.
Source: https://android.googlesource.com/platform/frameworks/support/+/a92b75a787d9ac93387f1e7ab9813ec9db43108a/test/uiautomator/uiautomator/src/main/java/androidx/test/uiautomator/UiDevice.java

Authoring status: source/diff validation only; fresh native recording and result.tsv are required. No Android pass or performance gain is claimed here.

Work is isolated in .local-lab/session-20260922-1600 on chatgpt/android-lab. Original main checkout, parallel uncommitted files, Blender4056946 history and ScanFlow emulator are unchanged. Measured free RAM1.62GB: no local Gradle/emulator started. Existing One Move Lab automation remains paused; no duplicate.
