#!/bin/bash
# Approves the CI runs of a weekly pull request that GitHub holds for a maintainer's approval.
#
# The weekly pull requests are opened by github-actions[bot], which counts as a first-time contributor until one of its
# pull requests has merged, so the repository's approval policy holds their pull_request runs ("action_required").
# Only those runs count as the pull request's checks: a workflow_dispatch run on the branch shows no checks on the
# pull request, so it can never satisfy the required "CI passed". Where approving is not allowed, the runs stay held
# for the weekly routine, which approves them as the maintainer (MAINTAINING.md).
#
# usage: approve_ci.sh <commit>   (needs GH_TOKEN and GH_REPO)
set -uo pipefail
sha="$1"
runs="repos/$GH_REPO/actions/runs?head_sha=$sha&event=pull_request"

held=""
for _ in $(seq 1 12); do # the runs appear a few seconds after the push
  held=$(gh api "$runs" --jq '.workflow_runs[] | select(.conclusion == "action_required") | .id')
  [ -n "$held" ] && break
  if [ "$(gh api "$runs" --jq '.total_count')" != 0 ]; then
    echo "CI runs on ${sha:0:12} without approval."
    exit 0
  fi
  sleep 10
done
[ -n "$held" ] || { echo "::warning::No CI run started for ${sha:0:12}."; exit 0; }

for id in $held; do
  if gh api -X POST "repos/$GH_REPO/actions/runs/$id/approve" >/dev/null 2>&1; then
    echo "Approved run $id."
  else
    echo "::notice::Could not approve run $id; the weekly routine approves it."
  fi
done
