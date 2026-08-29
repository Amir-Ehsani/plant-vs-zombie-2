#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PATCH_FILE="$SCRIPT_DIR/selection-screen-packets-font-spacing.patch"
COMMIT_MESSAGE="fix: refine plant selection packet visuals"

TARGET_A="core/src/main/java/screens/menu/AdventurePlantSelectionScreen.java"
TARGET_B="core/src/main/java/screens/menu/ZombotanyPlantSelectionScreen.java"

find_project_root() {
  local start="$1"
  local dir
  dir="$(cd "$start" 2>/dev/null && pwd)" || return 1

  while [[ "$dir" != "/" ]]; do
    if [[ -f "$dir/$TARGET_A" && -f "$dir/$TARGET_B" && -f "$dir/gradlew" ]]; then
      printf '%s\n' "$dir"
      return 0
    fi
    if [[ -f "$dir/pvz-proj/$TARGET_A" && -f "$dir/pvz-proj/$TARGET_B" && -f "$dir/pvz-proj/gradlew" ]]; then
      printf '%s\n' "$dir/pvz-proj"
      return 0
    fi
    dir="$(dirname "$dir")"
  done
  return 1
}

if [[ ! -f "$PATCH_FILE" ]]; then
  echo "ERROR: Patch file not found: $PATCH_FILE"
  echo "Keep this script and selection-screen-packets-font-spacing.patch in the same folder."
  exit 1
fi

PROJECT_ROOT=""
if PROJECT_ROOT="$(find_project_root "$PWD")"; then
  :
elif PROJECT_ROOT="$(find_project_root "$SCRIPT_DIR")"; then
  :
else
  echo "ERROR: Could not find pvz-proj root."
  echo "Run this script from pvz-proj (or keep it inside pvz-proj)."
  exit 1
fi

if ! GIT_ROOT="$(git -C "$PROJECT_ROOT" rev-parse --show-toplevel 2>/dev/null)"; then
  echo "ERROR: $PROJECT_ROOT is not inside a Git repository."
  exit 1
fi

PREFIX="$(git -C "$PROJECT_ROOT" rev-parse --show-prefix)"
PREFIX="${PREFIX%/}"

APPLY_DIR_ARGS=()
if [[ -n "$PREFIX" ]]; then
  APPLY_DIR_ARGS=(--directory="$PREFIX")
fi

GIT_TARGET_A="${PREFIX:+$PREFIX/}$TARGET_A"
GIT_TARGET_B="${PREFIX:+$PREFIX/}$TARGET_B"

echo "Project root : $PROJECT_ROOT"
echo "Git root     : $GIT_ROOT"
if [[ -n "$PREFIX" ]]; then
  echo "Git prefix   : $PREFIX"
else
  echo "Git prefix   : <repo root>"
fi

echo "[1/5] Checking patch..."
if git -C "$GIT_ROOT" apply --check "${APPLY_DIR_ARGS[@]}" "$PATCH_FILE"; then
  echo "[2/5] Applying patch..."
  git -C "$GIT_ROOT" apply --whitespace=fix "${APPLY_DIR_ARGS[@]}" "$PATCH_FILE"
elif git -C "$GIT_ROOT" apply --reverse --check "${APPLY_DIR_ARGS[@]}" "$PATCH_FILE"; then
  echo "[2/5] Patch is already applied; continuing."
else
  echo "ERROR: Patch does not match the current source files."
  echo "No commit was created."
  exit 1
fi

echo "[3/5] Compiling core..."
(
  cd "$PROJECT_ROOT"
  chmod +x ./gradlew 2>/dev/null || true
  ./gradlew :core:compileJava --no-daemon
)

echo "[4/5] Staging changed selection screens..."
git -C "$GIT_ROOT" add -- "$GIT_TARGET_A" "$GIT_TARGET_B"

if git -C "$GIT_ROOT" diff --cached --quiet -- "$GIT_TARGET_A" "$GIT_TARGET_B"; then
  echo "No new staged changes to commit (the patch may already be committed)."
  exit 0
fi

echo "[5/5] Committing..."
git -C "$GIT_ROOT" commit --only -m "$COMMIT_MESSAGE" -- "$GIT_TARGET_A" "$GIT_TARGET_B"

echo
echo "Done: $COMMIT_MESSAGE"
