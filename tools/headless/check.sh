#!/usr/bin/env bash
# Offline physics contracts only. Requires a Kotlin compiler and Java 21 on PATH.
# Value-type adapters replace Color/Offset/Rect; vibration is a no-op. No Android/UI claim.
set -euo pipefail
cd "$(dirname "$0")/../.."
command -v kotlinc >/dev/null || { echo "Kotlin compiler is required" >&2; exit 2; }
command -v java >/dev/null || { echo "Java is required" >&2; exit 2; }
mkdir -p build/headless
kotlinc tools/headless/stubs/*.kt \
 app/src/main/java/com/example/onemove/model/{Vector2D,PhysicsObjects,PinHitTester,Particle,LevelDefinition,LevelCatalog}.kt \
 app/src/main/java/com/example/onemove/ui/theme/OneMoveVisualTheme.kt \
 app/src/main/java/com/example/onemove/physics/{PhysicsWorld,SimulationState}.kt \
 app/src/test/java/com/example/{PhysicsContractCases,InputContractCases}.kt tools/headless/ContractMain.kt \
 -include-runtime -d build/headless/contracts.jar
java -Xmx768m -jar build/headless/contracts.jar
