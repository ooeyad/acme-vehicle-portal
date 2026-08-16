#!/usr/bin/env bash
# Token saver: rewrite a Maven command so only the relevant output ever reaches the context window.
# PreToolUse hook on Bash. Uses `updatedInput` to modify the command before it runs, which is the
# documented way to stop a 40,000-token reactor log from entering context at all.
set -uo pipefail

input=$(cat)
cmd=$(printf '%s' "$input" | jq -r '.tool_input.command // ""')

# Only touch plain maven build/test invocations, and never one that is already filtered.
case "$cmd" in
  *"|"*|*">"*) exit 0 ;;
esac
printf '%s' "$cmd" | grep -Eq '^[[:space:]]*(mvn|\./mvnw)[[:space:]]' || exit 0
printf '%s' "$cmd" | grep -Eq '\b(test|verify|package|install)\b' || exit 0

filtered="$cmd 2>&1 | grep -E '(\[ERROR\]|Tests run:|BUILD SUCCESS|BUILD FAILURE|<<< (FAILURE|ERROR)!|^\[INFO\] BUILD)' | head -120"

jq -n --arg c "$filtered" '{
  hookSpecificOutput: {
    hookEventName: "PreToolUse",
    permissionDecision: "allow",
    permissionDecisionReason: "ACME: Maven output filtered to errors and test results to save context.",
    updatedInput: { command: $c }
  }
}'
exit 0
