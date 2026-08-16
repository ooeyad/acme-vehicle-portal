#!/usr/bin/env bash
# SessionStart hook: tell Claude where we are in the release cycle before it does anything.
#
# During the freeze window ACME runs two workstreams at once - stabilising the release
# branch and developing the next iteration on develop - so "what is safe to do right now"
# depends on BOTH the date and the branch. This hook works that out and injects a short
# directive. Output goes into Claude's context, so it is kept deliberately brief.
#
# Reads .claude/release-calendar.json. Silent no-op if that file is absent.
set -uo pipefail

CAL="${CLAUDE_PROJECT_DIR:-.}/.claude/release-calendar.json"
[ -f "$CAL" ] || exit 0
command -v jq >/dev/null 2>&1 || exit 0

# ACME_SIM_DATE lets you rehearse any point in the release cycle without waiting for it.
# Unset in normal use. See .claude/bin/sim-cycle and simulation/DRILL.md.
TODAY="${ACME_SIM_DATE:-$(date +%Y-%m-%d)}"
if [ -n "${ACME_SIM_DATE:-}" ]; then
  printf 'SIMULATION MODE: pretending today is %s. Unset ACME_SIM_DATE for real dates.\n' "$TODAY"
fi
BRANCH=$(git -C "${CLAUDE_PROJECT_DIR:-.}" rev-parse --abbrev-ref HEAD 2>/dev/null || echo "unknown")

# Portable "days between two ISO dates". Falls back to empty if neither date flavour works.
epoch() {
  date -d "$1" +%s 2>/dev/null || date -j -f "%Y-%m-%d" "$1" +%s 2>/dev/null || echo ""
}
days_until() {
  local a b
  a=$(epoch "$1"); b=$(epoch "$TODAY")
  [ -z "$a" ] || [ -z "$b" ] && { echo ""; return; }
  echo $(( (a - b) / 86400 ))
}

WARN_DAYS=$(jq -r '.preFreezeWarningDays // 3' "$CAL")
TICKET=$(jq -r '.ticketPrefix // "#"' "$CAL")   # "#" for GitHub issues, "VP-" for Jira

# The active iteration is the first whose goLive has not passed (ISO dates compare lexically).
ACTIVE=$(jq -r --arg today "$TODAY" \
  '[.iterations[] | select(.goLive >= $today)] | sort_by(.goLive) | .[0] // empty' "$CAL")
[ -z "$ACTIVE" ] && exit 0

IT_NAME=$(printf '%s' "$ACTIVE"   | jq -r '.name')
FREEZE=$(printf '%s' "$ACTIVE"    | jq -r '.codeFreeze')
GOLIVE=$(printf '%s' "$ACTIVE"    | jq -r '.goLive')
REL_BRANCH=$(printf '%s' "$ACTIVE"| jq -r '.releaseBranch')
FIXVER=$(printf '%s' "$ACTIVE"    | jq -r '.fixVersion // .jiraFixVersion // .name')

# The iteration being developed on develop is the next one whose freeze is still ahead.
NEXT=$(jq -r --arg today "$TODAY" \
  '[.iterations[] | select(.codeFreeze > $today)] | sort_by(.codeFreeze) | .[0] // empty' "$CAL")
NEXT_NAME=$(printf '%s' "$NEXT" | jq -r '.name // "next"')
NEXT_FREEZE=$(printf '%s' "$NEXT" | jq -r '.codeFreeze // ""')

emit() { printf '%s\n' "$1"; }

case "$BRANCH" in
  main|master)
    emit "RELEASE CYCLE: you are on ${BRANCH}, which mirrors production. Do not edit files here."
    emit "Create a hotfix/${TICKET}<id>-slug branch from ${BRANCH}, or switch to develop."
    ;;

  release/*)
    D=$(days_until "$GOLIVE")
    emit "RELEASE CYCLE: STABILIZATION. Branch ${BRANCH}, iteration ${IT_NAME}, go-live ${GOLIVE}${D:+ (in ${D} days)}."
    emit "Only defect fixes for ${FIXVER} belong here. For this branch:"
    emit "- Every change needs an open bug issue. If there is no issue, stop and ask."
    emit "- Smallest safe fix only. No refactoring, no renaming, no dependency or build changes, no new features."
    emit "- Add a regression test that fails without the fix."
    emit "- After merging here the fix MUST be back-merged to develop or it regresses next iteration."
    emit "New feature work goes on develop for ${NEXT_NAME}, not here."
    ;;

  hotfix/*)
    emit "RELEASE CYCLE: HOTFIX off production. Branch ${BRANCH}."
    emit "Minimal fix plus a regression test. Nothing else in the diff."
    emit "Must be merged to main AND back-merged to develop and to ${REL_BRANCH} if that branch is still open."
    ;;

  develop|feature/*|"")
    D=$(days_until "${NEXT_FREEZE:-$FREEZE}")
    if [ -n "$D" ] && [ "$D" -le "$WARN_DAYS" ] && [ "$D" -ge 0 ]; then
      emit "RELEASE CYCLE: PRE-FREEZE. Code freeze for ${NEXT_NAME} is ${NEXT_FREEZE} (in ${D} days). Branch ${BRANCH}."
      emit "Land small and finishable work only. Do not start a multi-day change or a refactor that cannot merge before freeze."
      emit "If it will not be done and tested by then, say so and target ${NEXT_NAME}'s successor instead."
    else
      emit "RELEASE CYCLE: DEVELOPMENT for ${NEXT_NAME}. Branch ${BRANCH}. Code freeze ${NEXT_FREEZE}${D:+ (in ${D} days)}."
      emit "Feature work belongs here. Fixes for the iteration currently in test go on ${REL_BRANCH}, not here."
    fi
    ;;

  *)
    emit "RELEASE CYCLE: branch '${BRANCH}' does not match the ACME naming convention."
    emit "Expected feature/<id>-slug, bugfix/<id>-slug, hotfix/<id>-slug, develop, or release/<iteration>."
    emit "Confirm with the developer which branch this work belongs on before editing."
    ;;
esac
exit 0
