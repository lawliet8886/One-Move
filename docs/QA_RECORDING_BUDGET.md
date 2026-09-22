# Native recording budget correction

Observed run 35729119505 at 0aa2e9a: the first three native methods passed (wrong choices, 12-level campaign and retry). The lifecycle method started but the single 170-second host budget expired before any result. This run is NOT fully green and its 170-second video is not a complete four-test recording.

Split the same four methods into separately recorded invocations. Lifecycle runs first with 60 seconds, the 12-level campaign has 170 seconds, five causal wrong choices have 90 seconds, and retry has 45 seconds. Every method must produce explicit OK (1 test), normal completion and a non-empty actual video. No assertions or cases were removed. Result TSV records timeouts and failures individually; the full job fails if any check fails. Old evidence must not be relabelled.

The art renderer also failed on Ubuntu's Blender build without OpenImageDenoise. Denoising is disabled and sample count increased to 96. This is an offline renderer compatibility fix, not a claim that the art is complete or visually approved.
