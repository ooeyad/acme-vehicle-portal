#!/usr/bin/env bash
# ACME guardrail: block commands that must never run from an agent session.
# PreToolUse hook. Reads hook JSON on stdin; emits a deny decision or exits 0.
set -uo pipefail

input=$(cat)
cmd=$(printf '%s' "$input" | jq -r '.tool_input.command // ""')

deny() {
  jq -n --arg reason "$1" '{
    hookSpecificOutput: {
      hookEventName: "PreToolUse",
      permissionDecision: "deny",
      permissionDecisionReason: $reason
    }
  }'
  exit 0
}

# Production database access
if printf '%s' "$cmd" | grep -Eqi '\bdb2\b.*\bconnect\b.*(prod|prd)'; then
  deny "ACME policy: agent sessions may not connect to production DB2. Use the DEV or UAT alias."
fi

# DDL from the shell
if printf '%s' "$cmd" | grep -Eqi '\b(drop|truncate|alter)[[:space:]]+(table|index|tablespace|schema)\b'; then
  deny "ACME policy: DDL must be a reviewed migration file, not an ad-hoc command."
fi

# Force push / history rewrite on shared branches
if printf '%s' "$cmd" | grep -Eq 'git[[:space:]]+push[[:space:]].*(--force|-f)\b'; then
  deny "ACME policy: force push is not permitted from an agent session."
fi

# Pipe-to-shell installs
if printf '%s' "$cmd" | grep -Eq '(curl|wget)[^|]*\|[[:space:]]*(sudo[[:space:]]+)?(ba)?sh'; then
  deny "ACME policy: piping a downloaded script into a shell is not permitted."
fi

# Azure subscription switching
if printf '%s' "$cmd" | grep -Eq '\baz[[:space:]]+account[[:space:]]+set\b'; then
  deny "ACME policy: do not switch Azure subscriptions mid-session. Ask a human."
fi

exit 0
