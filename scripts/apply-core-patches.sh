#!/bin/sh
# Applies core-patches/*.patch to the core submodule, skipping any already applied. These carry core
# changes the Android app needs that aren't published in the Dayweave repository yet.
set -eu
cd "$(dirname "$0")/../core"
for patch in ../core-patches/*.patch; do
    [ -e "$patch" ] || continue
    if git apply --reverse --check "$patch" 2>/dev/null; then
        echo "Already applied: $patch"
    else
        git apply "$patch"
        echo "Applied: $patch"
    fi
done
