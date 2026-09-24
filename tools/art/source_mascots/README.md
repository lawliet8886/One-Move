# Source mascot atlases

These per-character WebP atlases and manifests are **production source/audit artifacts**, not Android runtime assets.

Runtime uses `app/src/main/assets/mascots/trio_runtime.webp` and `trio_runtime.json`, a lossless vertical stack of the reviewed Pip, Mochi, and Blobbo atlases.

The combined manifest preserves each source atlas SHA-256, framing, clip rectangles, approval state, and Tripo/rig provenance. Keeping these files under `tools/art` preserves reproducibility without duplicating roughly 419 KB inside every APK.

Measured on commit `c17dd41`: the debug APK changed from 17,514,816 bytes to 17,091,118 bytes when these six source files moved out of `main/assets`: **423,698 bytes saved (2.42%)**. The runtime trio atlas and decoded pixel budget are unchanged.
