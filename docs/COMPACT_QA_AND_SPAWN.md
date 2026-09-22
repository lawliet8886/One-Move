# Compact-screen QA and spawn separation

The new level6 initially placed neighbouring circles 65 units apart although their radii sum to66 and70. The solver resolved this tiny initial overlap but the authored spawn should not depend on that correction. Spread them to72 units, add a no-initial-overlap assertion, and rerun all160 counterweight JVM simulations. They passed with the stricter assertion. Production physics is unchanged.

Add a fifth native case at720x1280 pixels,320dpi and1.3x font scale: visible48dp reset/menu targets, wrong-pin failure, retry, rescue, next level, level selection and return. Screen and font settings apply only to the disposable CI emulator and are restored after recording, including on error. These checks are newly authored and require a fresh native run; do not call them passed from syntax inspection.

Eight other boards still retain recovery-funnel layouts. Current physical fairness and art improvement are not a final difficulty curve or a guarantee of no bugs.
