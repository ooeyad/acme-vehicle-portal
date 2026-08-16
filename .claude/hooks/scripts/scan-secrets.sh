#!/usr/bin/env bash
# ACME guardrail: flag likely secrets immediately after Claude writes a file.
# PostToolUse hook. Cannot block (PostToolUse ignores exit 2 for blocking),
# but exit 2 surfaces stderr to Claude so it corrects itself in the same turn.
set -uo pipefail

input=$(cat)
file=$(printf '%s' "$input" | jq -r '.tool_input.file_path // ""')
[ -z "$file" ] && exit 0
[ -f "$file" ] || exit 0

patterns='(password|passwd|pwd|secret|api[_-]?key|access[_-]?token|client[_-]?secret)[[:space:]]*[=:][[:space:]]*["'"'"']?[A-Za-z0-9/+_.-]{8,}'
db2conn='jdbc:db2://[^"'"'"'[:space:]]*(user|password)='
azure='(DefaultEndpointsProtocol=.*AccountKey=|SharedAccessSignature=)'

hits=$(grep -nEi "$patterns|$db2conn|$azure" "$file" 2>/dev/null | grep -viE '(getenv|System\.getProperty|process\.env|\$\{|<<|placeholder|example|CHANGEME|\*\*\*)' | head -5)

if [ -n "$hits" ]; then
  {
    echo "ACME secret scan flagged $file:"
    echo "$hits"
    echo ""
    echo "Move this value to an environment variable or the Azure Key Vault reference used elsewhere in this repo, then re-check."
  } >&2
  exit 2
fi
exit 0
