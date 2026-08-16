#!/usr/bin/env bash
# PreToolUse hook on Bash: refuse to commit while standing on a protected branch.
#
# WHY: push protection (GitHub branch rules, or the deny rules in settings.json) only
# catches this at push time. A developer who commits for an hour on the wrong branch
# doesn't find out until the push is rejected, and then has to untangle it. This catches
# it at the first commit instead.
#
# Merges are deliberately still allowed: the release manager legitimately merges into
# develop and main. `git merge --no-ff` creates its commit directly and never reaches
# this hook; a conflicted merge finalised with `git commit` is detected via MERGE_HEAD
# and permitted.
set -uo pipefail

input=$(cat)
cmd=$(printf '%s' "$input" | jq -r '.tool_input.command // ""')

# Only interested in commits.
printf '%s' "$cmd" | grep -Eq '(^|[;&|]|\s)git\s+(-[^ ]+\s+)*commit(\s|$)' || exit 0

REPO="${CLAUDE_PROJECT_DIR:-.}"
BRANCH=$(git -C "$REPO" rev-parse --abbrev-ref HEAD 2>/dev/null) || exit 0

case "$BRANCH" in
  develop|main|master|release/*) ;;
  *) exit 0 ;;
esac

# Finalising a merge or a cherry-pick is legitimate on these branches.
if git -C "$REPO" rev-parse -q --verify MERGE_HEAD >/dev/null 2>&1 \
   || git -C "$REPO" rev-parse -q --verify CHERRY_PICK_HEAD >/dev/null 2>&1; then
  exit 0
fi

jq -n --arg b "$BRANCH" '{
  hookSpecificOutput: {
    hookEventName: "PreToolUse",
    permissionDecision: "deny",
    permissionDecisionReason:
      ("ACME policy: no direct commits to " + $b + ". Work belongs on a branch.\n\n"
       + "If nothing is committed yet:\n"
       + "  git checkout -b <feature|bugfix|hotfix>/<issue>-<slug>\n\n"
       + "If you already committed here by mistake, move the commits to a branch:\n"
       + "  git branch <type>/<issue>-<slug>        # label the commits\n"
       + "  git reset --hard origin/" + $b + "      # rewind this branch (or @{u}, or the last good SHA)\n"
       + "  git checkout <type>/<issue>-<slug>\n\n"
       + "Merges are still allowed - this only blocks direct commits.")
  }
}'
exit 0
