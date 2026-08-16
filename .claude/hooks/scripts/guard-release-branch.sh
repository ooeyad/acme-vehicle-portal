#!/usr/bin/env bash
# PreToolUse hook on Edit|Write: keep the freeze window narrow.
#
# On a release/* branch only defect fixes are allowed. The classes of change below are
# never a defect fix, and each one has caused a released regression at least once, so
# they are blocked rather than discouraged. A human can still make them directly.
set -uo pipefail

input=$(cat)
file=$(printf '%s' "$input" | jq -r '.tool_input.file_path // ""')
[ -z "$file" ] && exit 0

BRANCH=$(git -C "${CLAUDE_PROJECT_DIR:-.}" rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
case "$BRANCH" in
  release/*) ;;
  *) exit 0 ;;
esac

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

base=$(basename "$file")
case "$base" in
  pom.xml|package.json|package-lock.json|build.gradle|ivy.xml)
    deny "FREEZE WINDOW: ${BRANCH} is in stabilization. Dependency and build changes are not defect fixes and are the most common cause of a late-breaking release failure. Land this on develop for the next iteration." ;;
  struts-config.xml|web.xml)
    deny "FREEZE WINDOW: ${BRANCH} is in stabilization. Adding or rewiring actions changes application structure. If this is genuinely a defect fix, a human makes the change and notes why in the ticket." ;;
  azure-pipelines.yml|Jenkinsfile|bitbucket-pipelines.yml)
    deny "FREEZE WINDOW: ${BRANCH} is in stabilization. Do not change the build or deploy pipeline for a release that is already in test." ;;
esac

case "$file" in
  db/migration/*|*/db/migration/*)
    deny "FREEZE WINDOW: a new DB2 migration after code freeze needs the release manager and a DBA. Not an agent change." ;;
esac

exit 0
