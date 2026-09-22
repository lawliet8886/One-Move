# Rescue UI ergonomics — 22 September 2026

Baseline e36fe48deab510be36a29f9db6d70331590697ca, Android run35744860514. Downloaded native result.tsv: all five methods passed. This includes six causal prototype boards, the real weight switch and eleven wrong choices. The local art merge f0aadaf and original4056946 are ancestors of this baseline and must be preserved.

## Candidate, not yet native approval

The old level-selection overlay had no Android Back handler. Back now closes only the sheet without resetting the game or leaving the Activity. The HUD can reflow at large font sizes instead of squeezing portraits. Result panels reuse the existing original Blender atlas at a readable portrait size; only the explanation scrolls, keeping Retry/Next anchored. The selector now shows level names on enamel/brass cards, with two columns at fontScale>=1.6 and content-sized tiles instead of fixed76dp tiles.

No physics, solution key, pin hit testing, authored geometry, sprites or Blender sources changed. Portrait bodies are copies, not relocated simulation objects. The original five native cases remain; two additional recorded cases exercise system Back and a narrower640x1136/320dpi/200% font device, including rapid repeated touches, real failure, retry, success and phase change. These are scripted regression tests, not free exploration. Require their actual reports and screenshots before accepting this candidate.

## Resource and evidence limits

Desktop Commander initially answered list_devices and get_config, then start_process/ping/read_file timed out; a subsequent harmless write probe returned No devices available. No local source edit, installation, emulator use, process termination or resource measurement was confirmed. Continue via GitHub rather than guessing Windows state. The other project's ScanFlow emulator remains prohibited.

Current cloud-emulator campaign gfxinfo at e36fe48:817 rendered frames,817 janky,200ms median. This is not a physical-phone FPS figure or a controlled speedup claim. Performance still needs work. Six boards still retain recovery funnels. This UI change is not a completed difficulty curve or a claim of expressive 3D throughout the game.

Automation6ab23f579b008191a7a6dfe162c341ac was temporarily paused to avoid simultaneous editing. Resume with the final verified head and remaining limitations after this interactive session. Never force-push, touch main, duplicate the automation, spend Tripo credits or invoke paid inference/Codex.
