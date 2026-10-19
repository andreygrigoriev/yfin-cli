#!/usr/bin/env bash
# Live smoke test: runs a native (or any) yfin binary against Yahoo and checks the JSON shape of
# a few commands. It asserts exit codes, keys and value types, never values.
#
# Usage: scripts/live-smoke.sh BINARY
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "usage: $0 BINARY" >&2
  exit 2
fi
bin=$1
command -v jq >/dev/null || { echo "jq is required" >&2; exit 2; }

failures=0
out=$(mktemp)
trap 'rm -f "$out"' EXIT

# check NAME FILTER ARGS...: runs `BINARY ARGS... --format json`, requires exit 0 and that the jq
# FILTER evaluates to true on the output.
check() {
  local name=$1 filter=$2
  shift 2
  local code=0
  "$bin" "$@" --format json >"$out" || code=$?
  if [[ $code -ne 0 ]]; then
    echo "FAIL $name: exit $code" >&2
    failures=$((failures + 1))
  elif ! jq -e "$filter" "$out" >/dev/null; then
    echo "FAIL $name: unexpected shape" >&2
    head -c 2000 "$out" >&2
    echo >&2
    failures=$((failures + 1))
  else
    echo "ok   $name"
  fi
}

# helpers shared by the filters: a non-empty array of objects, and "key holds a number" where
# missing values render as null
defs='
  def rows: type == "array" and length > 0 and all(.[]; type == "object");
  def num(k): (.[k] | type) as $t | $t == "number" or $t == "null";
  def str(k): (.[k] | type) == "string";
'

check "quote" "$defs"'
  rows and length == 2
  and ([.[].symbol] | sort) == ["AAPL", "MSFT"]
  and all(.[]; (.last_price | type) == "number" and num("change") and num("change_pct")
              and num("volume") and num("market_cap") and str("currency"))
' quote AAPL MSFT

check "history" "$defs"'
  rows
  and all(.[]; str("date") and (.close | type) == "number" and num("open") and num("high")
              and num("low") and num("adj_close") and (.volume | type) == "number")
' history AAPL --period 1mo

check "info" "$defs"'
  type == "object" and .symbol == "AAPL" and str("quote_type") and str("currency")
  and (.market_cap | type) == "number" and num("trailing_pe") and num("current_price")
' info AAPL

check "financials income" "$defs"'
  rows
  and all(.[]; str("line_item"))
  and (.[0] | keys - ["line_item"] | length > 0
       and all(.[]; test("^[0-9]{4}_[0-9]{2}_[0-9]{2}$")))
  and all(.[]; to_entries | all(.[]; .key == "line_item" or (.value | type) == "number"
                                     or (.value | type) == "null"))
  and any(.[]; to_entries | any(.[]; (.value | type) == "number"))
' financials income AAPL

check "options" "$defs"'
  rows and all(.[]; str("expiry") and (.expiry | test("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")))
' options AAPL

check "search" "$defs"'
  rows and all(.[]; str("symbol") and num("score")) and any(.[]; .symbol == "AAPL")
' search apple

if [[ $failures -gt 0 ]]; then
  echo "$failures check(s) failed" >&2
  exit 1
fi
echo "all live checks passed"
