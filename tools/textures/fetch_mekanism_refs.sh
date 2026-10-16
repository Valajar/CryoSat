#!/usr/bin/env bash
# Sparse clone of Mekanism's textures (MIT licensed) for style reference. Not committed.
# Usage: tools/textures/fetch_mekanism_refs.sh [DEST]   (default: .mekanism-refs in the repo root)
set -euo pipefail
DEST="${1:-$(git rev-parse --show-toplevel)/.mekanism-refs}"
if [ -d "$DEST/.git" ]; then
  echo "$DEST already exists"
  exit 0
fi
git clone --depth 1 --filter=blob:none --sparse --branch 1.21.x https://github.com/mekanism/Mekanism "$DEST"
git -C "$DEST" sparse-checkout set --no-cone \
  '/src/main/resources/assets/mekanism/textures/' \
  '/src/main/resources/assets/mekanism/models/' \
  '/src/generators/resources/assets/mekanismgenerators/textures/'
echo "Mekanism references in $DEST/src/main/resources/assets/mekanism/textures"
