#!/usr/bin/env bash
# Block edits to paths that must stay human-owned in the legacy estate.
# PreToolUse hook on Edit|Write.
set -uo pipefail

input=$(cat)
file=$(printf '%s' "$input" | jq -r '.tool_input.file_path // ""')
[ -z "$file" ] && exit 0

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

case "$file" in
  db/migration/*|*/db/migration/*|*/src/main/resources/db/*)
    deny "ACME policy: DB2 migration files are human-authored and DBA-reviewed. Propose the DDL in your summary instead of writing the file." ;;
  target/*|*/target/*|build/*|*/build/*|*/WebContent/vendor/*|node_modules/*|*/node_modules/*)
    deny "ACME policy: generated, built, or vendored files are not editable. Change the source instead." ;;
  *-generated.java|*Generated.java|*.class)
    deny "ACME policy: generated Java is not editable. Change the generator input." ;;
  web.xml|*/web.xml)
    deny "ACME policy: web.xml controls the security filter chain and is changed by a human with security review." ;;
esac

exit 0
