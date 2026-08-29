#!/usr/bin/env bash
set -e

PATCH="ui-polish-notifications-chapter-zombie.patch"
PREFIX="$(git rev-parse --show-prefix)"

if [ -n "$PREFIX" ]; then
  git apply --directory="${PREFIX%/}" "$PATCH"
else
  git apply "$PATCH"
fi

git add -A
git commit -m "feat: polish selection, chapters and UI feedback"
