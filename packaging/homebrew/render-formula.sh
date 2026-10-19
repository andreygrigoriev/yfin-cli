#!/usr/bin/env bash
# Renders the Homebrew formula for one release to stdout.
# Usage: render-formula.sh VERSION SHA256SUMS
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "usage: $0 VERSION SHA256SUMS" >&2
  exit 2
fi
version=$1
sums=$2
template="$(dirname "$0")/yfin.rb.tmpl"

# sha256 of one tarball from a `sha256sum` listing ("<hash>  <file>" or "<hash> *<file>")
sha_of() {
  local file="yfin-${version}-$1.tar.gz" sha
  sha=$(awk -v f="$file" '{ name = $2; sub(/^\*/, "", name) } name == f { print $1 }' "$sums")
  if [[ ! $sha =~ ^[0-9a-f]{64}$ ]]; then
    echo "render-formula: no sha256 for $file in $sums" >&2
    exit 1
  fi
  echo "$sha"
}

darwin_arm64=$(sha_of darwin-arm64)
darwin_x64=$(sha_of darwin-x64)
linux_arm64=$(sha_of linux-arm64)
linux_x64=$(sha_of linux-x64)

sed -e "s/@VERSION@/${version}/g" \
  -e "s/@SHA256_DARWIN_ARM64@/${darwin_arm64}/" \
  -e "s/@SHA256_DARWIN_X64@/${darwin_x64}/" \
  -e "s/@SHA256_LINUX_ARM64@/${linux_arm64}/" \
  -e "s/@SHA256_LINUX_X64@/${linux_x64}/" \
  "$template"
