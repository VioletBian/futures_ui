#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CODEX_HOME_DIR="${CODEX_HOME:-$HOME/.codex}"
SOURCE_DIR="$CODEX_HOME_DIR/skills"
DEST_DIR="$ROOT_DIR/codex-skills/skills"
INCLUDE_SYSTEM=0

if [[ "${1:-}" == "--include-system" ]]; then
  INCLUDE_SYSTEM=1
fi

if [[ ! -d "$SOURCE_DIR" ]]; then
  echo "Source skills directory not found: $SOURCE_DIR" >&2
  exit 1
fi

mkdir -p "$DEST_DIR"
find "$DEST_DIR" -mindepth 1 -maxdepth 1 -exec rm -rf {} +

for entry in "$SOURCE_DIR"/* "$SOURCE_DIR"/.*; do
  name="$(basename "$entry")"

  if [[ "$name" == "." || "$name" == ".." ]]; then
    continue
  fi

  if [[ ! -e "$entry" ]]; then
    continue
  fi

  if [[ "$name" == ".system" && "$INCLUDE_SYSTEM" -ne 1 ]]; then
    continue
  fi

  cp -R "$entry" "$DEST_DIR/$name"
done

echo "Exported skills to $DEST_DIR"
