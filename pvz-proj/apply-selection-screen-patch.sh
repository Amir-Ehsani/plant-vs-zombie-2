#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PATCH_FILE="$SCRIPT_DIR/selection-screen-packets-font-spacing.patch"
COMMIT_MESSAGE="fix: refine plant selection packet visuals"

if ! ROOT="$(git rev-parse --show-toplevel 2>/dev/null)"; then
  echo "ERROR: Run this script from inside the pvz-proj Git repository."
  exit 1
fi

cd "$ROOT"

if [[ ! -f "$PATCH_FILE" ]]; then
  echo "ERROR: Patch file not found: $PATCH_FILE"
  echo "Keep this script and the .patch file in the same folder."
  exit 1
fi

echo "[1/5] Checking patch..."
git apply --check "$PATCH_FILE"

echo "[2/5] Applying patch..."
git apply --whitespace=fix "$PATCH_FILE"

echo "[3/5] Compiling core..."
chmod +x ./gradlew 2>/dev/null || true
./gradlew :core:compileJava --no-daemon

echo "[4/5] Staging changed selection screens..."
git add -- \
  core/src/main/java/screens/menu/AdventurePlantSelectionScreen.java \
  core/src/main/java/screens/menu/ZombotanyPlantSelectionScreen.java

echo "[5/5] Committing..."
git commit --only -m "$COMMIT_MESSAGE" -- \
  core/src/main/java/screens/menu/AdventurePlantSelectionScreen.java \
  core/src/main/java/screens/menu/ZombotanyPlantSelectionScreen.java

echo
echo "Done: $COMMIT_MESSAGE"
