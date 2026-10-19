#!/usr/bin/env bash
# Renders the formula from a fake SHA256SUMS and checks the result. Run by ci.yml.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

version=1.2.3
targets=(darwin-arm64 darwin-x64 linux-arm64 linux-x64)
for target in "${targets[@]}"; do
  sha=$(printf '%s' "$target" | shasum -a 256)
  echo "${sha%% *}  yfin-${version}-${target}.tar.gz"
done >"$work/SHA256SUMS"
echo "deadbeef  unrelated.txt" >>"$work/SHA256SUMS"

"$here/render-formula.sh" "$version" "$work/SHA256SUMS" >"$work/yfin.rb"

fail() {
  echo "test-render: $*" >&2
  exit 1
}

if grep -qE '@[A-Z0-9_]+@' "$work/yfin.rb"; then
  fail "unreplaced placeholder"
fi
grep -q "version \"${version}\"" "$work/yfin.rb" || fail "version missing"
for target in "${targets[@]}"; do
  url="https://github.com/andreygrigoriev/yfin-cli/releases/download/v${version}/yfin-${version}-${target}.tar.gz"
  sha=$(awk -v f="yfin-${version}-${target}.tar.gz" '$2 == f { print $1 }' "$work/SHA256SUMS")
  grep -qF "url \"${url}\"" "$work/yfin.rb" || fail "url missing for ${target}"
  grep -qF "sha256 \"${sha}\"" "$work/yfin.rb" || fail "sha256 missing for ${target}"
done
# `yfin completion` takes no arguments: Homebrew must not append the shell name
grep -qF 'shell_parameter_format: :none' "$work/yfin.rb" || fail "completion would get a shell argument"
[[ $(grep -c '^ *url "' "$work/yfin.rb") -eq 4 ]] || fail "expected 4 urls"
[[ $(grep -c '^ *sha256 "' "$work/yfin.rb") -eq 4 ]] || fail "expected 4 sha256s"

# a missing target must fail the render
grep -v linux-x64 "$work/SHA256SUMS" >"$work/SHA256SUMS.partial"
if "$here/render-formula.sh" "$version" "$work/SHA256SUMS.partial" >/dev/null 2>&1; then
  fail "render succeeded with a missing target"
fi

ruby -c "$work/yfin.rb" >/dev/null || fail "ruby -c failed"
echo "test-render: ok"
