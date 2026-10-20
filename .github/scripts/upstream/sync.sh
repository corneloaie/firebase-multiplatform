#!/bin/bash
# Merges upstream GitLive into the checked-out branch: their v3.0.0 line, then every upstream pull request branch this
# fork carries ahead of upstream (.github/upstream-merges.tsv), re-merged when its head has moved and dropped once
# upstream has merged the pull request into v3.0.0.
#
# Only mechanical conflicts are resolved here: README.md (this fork's own), Apple sources whose only upstream-side
# change is the CocoaPods -> SwiftPM import rewrite (resolve_apple.py), and version pins that both sides moved
# (resolve_versions.py: the higher version wins, androidSdk.apiRef and forkVersion stay this fork's). Anything else
# aborts that merge and is listed in the summary for a person (or Claude) to resolve by hand.
#
# usage: sync.sh <summary.md>   (needs GH_TOKEN for `gh`; exit 0 = everything merged, 3 = some merges need a hand)
set -uo pipefail
summary="$1"
here="$(cd "$(dirname "$0")" && pwd)"
tracked=.github/upstream-merges.tsv
upstream_repo=GitLiveApp/firebase-kotlin-sdk
needs_hand=()
merged=()

git remote get-url upstream >/dev/null 2>&1 || git remote add upstream "https://github.com/$upstream_repo.git"
git fetch --quiet --no-tags upstream '+refs/heads/*:refs/remotes/upstream/*'

# merge <ref> <message> [module]: merges <ref>, applying the mechanical resolutions; aborts and returns 1 on anything else.
merge() {
  local ref="$1" message="$2" module="${3:-}"
  if git merge --no-ff --no-edit -m "$message" "$ref" >/dev/null 2>&1; then return 0; fi
  if git diff --name-only --diff-filter=U | grep -qx README.md; then git checkout --ours README.md && git add README.md; fi
  if [ -n "$module" ]; then python3 "$here/resolve_apple.py" "$ref" "$module" >/dev/null 2>&1 || true; fi
  local pins
  pins="$(git diff --name-only --diff-filter=U | grep -E '(^gradle/libs\.versions\.toml|^gradle\.properties|/build\.gradle\.kts)$' || true)"
  if [ -n "$pins" ]; then python3 "$here/resolve_versions.py" $pins >/dev/null 2>&1 || true; fi
  local left
  left="$(git diff --name-only --diff-filter=U)"
  if [ -z "$left" ]; then git add -A && git commit --no-edit -q && return 0; fi
  needs_hand+=("**$ref**: $(echo "$left" | sed 's/^/`/;s/$/`/' | paste -sd ',' - | sed 's/,/, /g')")
  git merge --abort
  return 1
}

before=$(git rev-parse HEAD)
v3=$(git rev-parse upstream/v3.0.0)
if git merge-base --is-ancestor "$v3" HEAD; then
  merged+=("GitLive \`v3.0.0\` is already merged (\`${v3:0:12}\`).")
elif merge upstream/v3.0.0 "Merge upstream GitLive v3.0.0 (${v3:0:12})"; then
  merged+=("Merged GitLive \`v3.0.0\` at \`${v3:0:12}\`: $(git log --oneline "$before..upstream/v3.0.0" | wc -l | tr -d ' ') new upstream commits.")
fi

updated_tsv=$(mktemp)
while IFS=$'\t' read -r branch pr sha; do
  if [[ -z "$branch" || "$branch" == \#* ]]; then printf '%s\n' "$branch${pr:+	$pr}${sha:+	$sha}" >> "$updated_tsv"; continue; fi
  state=$(gh pr view "$pr" -R "$upstream_repo" --json state --jq .state 2>/dev/null || echo UNKNOWN)
  head=$(git rev-parse -q --verify "upstream/$branch" 2>/dev/null || echo "")
  if [ "$state" = MERGED ] && { [ -z "$head" ] || git merge-base --is-ancestor "$head" upstream/v3.0.0; }; then
    merged+=("[#$pr](https://github.com/$upstream_repo/pull/$pr) (\`$branch\`) is merged upstream into v3.0.0: no longer tracked.")
    continue
  fi
  if [ "$state" = CLOSED ]; then
    needs_hand+=("[#$pr](https://github.com/$upstream_repo/pull/$pr) (\`$branch\`) was closed upstream without merging: decide whether to keep its changes.")
  elif [ -n "$head" ] && [ "$head" != "$sha" ] && ! git merge-base --is-ancestor "$head" HEAD; then
    module=${branch%-source-compat}
    if merge "upstream/$branch" "Merge upstream PR #$pr update ($branch @ ${head:0:12})" "$module"; then
      merged+=("Re-merged [#$pr](https://github.com/$upstream_repo/pull/$pr) (\`$branch\`): \`${sha:0:12}\` -> \`${head:0:12}\`.")
      sha="$head"
    fi
  fi
  printf '%s\t%s\t%s\n' "$branch" "$pr" "$sha" >> "$updated_tsv"
done < "$tracked"
if ! cmp -s "$updated_tsv" "$tracked"; then
  cp "$updated_tsv" "$tracked"
  git add "$tracked" && git commit -q -m "Track the upstream PR heads merged by the weekly sync"
fi

{
  echo "### Upstream GitLive sync"
  echo
  for line in "${merged[@]}"; do echo "- $line"; done
  [ ${#merged[@]} -eq 0 ] && echo "- Nothing new upstream."
  if [ ${#needs_hand[@]} -gt 0 ]; then
    echo
    echo "### Needs a manual merge"
    echo
    for line in "${needs_hand[@]}"; do echo "- $line"; done
    echo
    echo "Merge these locally (\`git fetch upstream && git merge upstream/<branch>\`); \`.github/scripts/upstream/resolve_apple.py\` handles the CocoaPods -> SwiftPM import conflicts."
  fi
} > "$summary"

[ ${#needs_hand[@]} -eq 0 ] || exit 3
