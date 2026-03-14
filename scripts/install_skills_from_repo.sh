#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CODEX_HOME_DIR="${CODEX_HOME:-$HOME/.codex}"
SOURCE_DIR="$ROOT_DIR/codex-skills/skills"
DEST_DIR="$CODEX_HOME_DIR/skills"

if [[ ! -d "$SOURCE_DIR" ]]; then
  echo "Tracked skills directory not found: $SOURCE_DIR" >&2
  exit 1
fi

mkdir -p "$DEST_DIR"

for entry in "$SOURCE_DIR"/* "$SOURCE_DIR"/.*; do
  name="$(basename "$entry")"

  if [[ "$name" == "." || "$name" == ".." ]]; then
    continue
  fi

  if [[ ! -e "$entry" ]]; then
    continue
  fi

  rm -rf "$DEST_DIR/$name"
  cp -R "$entry" "$DEST_DIR/$name"
done

echo "Installed tracked skills into $DEST_DIR"
