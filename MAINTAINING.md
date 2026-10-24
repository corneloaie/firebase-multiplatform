# Maintaining firebase-multiplatform

This fork of [GitLive's firebase-kotlin-sdk](https://github.com/GitLiveApp/firebase-kotlin-sdk) tracks upstream closely.
Everything that makes it a separate project is kept in a few places, so that the weekly upstream merges stay
mechanical:

| What | Where |
|---|---|
| Maven coordinates, version, POM | `buildSrc/src/main/kotlin/fork/Fork.kt`, `forkVersion` in `gradle.properties` |
| Upstream PRs merged ahead of upstream | `.github/upstream-merges.tsv` |
| Weekly automation | `.github/workflows/weekly.yml`, `.github/scripts/` |
| Dependency updates | `.claude/skills/dependency-update/` (the routine's procedure, `check_updates.py`, `pins.txt`) |
| Publishing | `.github/workflows/publish.yml` |
| Docs | `README.md` (this fork's own, kept on upstream merges), `NOTICE`, this file |

Do not rename the Kotlin packages (`dev.gitlive.firebase.*`, `com.google.firebase.*`), or the Gradle `group`
(`dev.gitlive`). Renaming either turns every upstream merge into conflicts. The group also names the SwiftPM cinterop
packages (`swiftPMImport.dev.gitlive.firebase.<module>`) that every Apple source imports.

## Branches and remotes

- `main` is the default branch and what gets released.
- `upstream` is GitLive (`https://github.com/GitLiveApp/firebase-kotlin-sdk.git`). This fork follows its `v3.0.0` line,
  which uses SwiftPM, Kotlin 2.4 and Wasm, not its `master` (2.x, CocoaPods).
- `weekly/upstream-sync` and `weekly/firebase-sdks` are rewritten by the weekly workflow, one pull request each. A
  commit pushed to one of them is lost the next Monday unless its pull request has merged.
- `deps/weekly` is the dependency routine's branch, one pull request. The routine deletes it once that pull request has
  merged and starts the next one from `main`.

## The weekly workflow

It runs every Monday at 05:23 UTC, and on demand from the Actions tab (`Weekly update`, `Run workflow`).

### 1. Upstream merge (`.github/scripts/upstream/sync.sh`)

- Merges `upstream/v3.0.0`.
- For each line in `.github/upstream-merges.tsv` (branch, upstream PR number, merged head):
  - if the PR is merged into `v3.0.0` upstream, it drops the line;
  - if the branch head moved, it merges the new head and records it.
- Conflicts in `README.md` keep this fork's version.
- This fork deleted `.github/dependabot.yml` (see "Dependency updates" below), so an upstream edit to it keeps it
  deleted. The edit is listed under "To check" in the upstream pull request: move a new ignore entry into `pins.txt`.
- Apple sources whose only upstream-side change is the CocoaPods to SwiftPM import rewrite are resolved by
  `.github/scripts/upstream/resolve_apple.py`.
- Any other conflict aborts that merge and is reported in the issue "Upstream GitLive changes need a manual merge". Then
  resolve it by hand:

```bash
git switch -c merge-upstream main
git fetch upstream
git merge upstream/<branch>
python3 .github/scripts/upstream/resolve_apple.py upstream/<branch> <module>
```

When upstream lands a pull request this fork merged early, expect the same files to conflict once. Upstream's version
is usually the one to keep, except for this fork's Wasm actuals: upstream may write its own, and then theirs win.

### 2. Firebase SDK update (`.github/scripts/weekly/firebase_versions.py`, `coverage.py`)

It moves these four to their newest stable release:

| What | Where |
|---|---|
| `firebase-bom` | `gradle/libs.versions.toml` |
| `firebase-ios-sdk` | `gradle/libs.versions.toml`, the SwiftPM minimum version; also the minimum of the Objective-C wrapper packages (`*/apple/*/Package.swift`) |
| the `firebase` npm pins | the module build files |
| `androidSdk.apiRef` | `gradle.properties`, the newest `m<NNN> mergeback` commit of firebase-android-sdk, i.e. the API of the latest Firebase Android release. The mergeback can land days after a release: to cover a release sooner, set it by hand to the head of its `releases/m<NNN>.release` branch, which the update keeps until a newer mergeback contains it |

It then runs `updateAndroidSdkApi`, regenerates the Firestore pipeline expressions (below), runs
`androidSourceCompatDump` and refreshes the README table. The pull request body lists every member that became `MISS` or
`OMIT`. A major version bump usually needs code changes; CI on the pull request shows where.

The ~650 functions of the Firestore pipeline API's `Expression` and its aggregate functions are generated:
`python3 firebase-firestore/codegen/generate_pipeline.py` reads the Android SDK's `expressions.kt` and `aggregates.kt` at
`androidSdk.apiRef` and writes `Expression.kt` and `AggregateFunction.kt` in
`firebase-firestore/src/commonMain/kotlin/com/google/firebase/firestore/pipeline/`, keeping each function's name,
parameters and the backend function it builds. It fails on a function it cannot translate: add an override for it in
the script (`OVERRIDES`). The rest of the pipeline API (stages, options, `Pipeline`, the expression classes) is written
by hand, and the Android compilation checks all of it against the SDK's classes.

### 3. The weekly routine

A Claude Code routine on the maintainer's Mac (a scheduled task in the Claude desktop app) finishes the update. It runs
on Mondays after the workflow and again on Tuesdays, as the maintainer in their own checkout, so the repository needs no
Claude token. Each run picks up where the update stands:

- It implements, on every platform, the members the SDK pull request lists as newly missing.
- It fixes what the new SDKs or upstream changes broke, and pushes to the weekly branches.
- It resolves the merges reported in "Upstream GitLive changes need a manual merge".
- It approves the weekly pull requests' CI runs if GitHub still holds them for approval, and re-runs CI jobs that failed
  for transient reasons.
- It arms auto-merge (a merge commit) on a weekly pull request once coverage is 100% and no public API was removed, and
  comments on what it did. The workflow turns auto-merge off again whenever it refreshes a pull request.

It never releases. Without the routine, do the same by hand: see "Closing coverage gaps by hand" below.

## Dependency updates

Everything the weekly workflow doesn't move is kept current by a second Claude Code routine on the maintainer's Mac,
which replaced Dependabot. It covers:
- the version catalog;
- versions hard-coded in build scripts;
- the Gradle wrapper;
- the GitHub Actions of `.github/workflows` and `.github/actions`.

Its procedure is the repository skill [`.claude/skills/dependency-update`](.claude/skills/dependency-update/SKILL.md),
so a change to it is reviewed like any other. It runs on Wednesdays and Thursdays, away from the Monday and Tuesday
weekly pull requests, which edit the same catalog:

- **Wednesday:** `check_updates.py` lists what is newer. The routine bumps it in groups (Actions; patches and minors by
  family; Kotlin; then each major on its own), checks each group locally, and pushes one pull request from
  `deps/weekly`, "Weekly: dependency updates".
- **Thursday:** it reads that pull request's CI. It re-runs transient failures, and fixes or reverts the group that broke
  something.
- **Majors:** it tries each one on its own, and holds it back if it can't make it pass, taking the newest version of
  the current major instead.
- **Auto-merge:** it arms auto-merge (a merge commit) once no public API was removed and nothing it bumped needs a
  decision.

[`pins.txt`](.claude/skills/dependency-update/pins.txt) says what it must not bump, and why:
- `<X.Y` stays below a version line (kotlinx-coroutines below 1.11, dokka below 2.2);
- `hold` is left for a person (dokka and the publish plugin: pull request CI never publishes, so they need a Publish
  dry run);
- `weekly` is the weekly workflow's (the Firebase SDKs).

Remove a pin in the pull request that makes its reason go away.

To see what is outdated without the routine, run:

```bash
python3 .claude/skills/dependency-update/check_updates.py
```

## Closing coverage gaps by hand

```bash
./gradlew :firebase-messaging:androidSourceCompatDump   # regenerate the report
grep -E "^  (MISS|OMIT)" firebase-messaging/api/android-sdk-compat.txt
```

Implement each member in the module's `com.google.firebase` layer on every platform, following
`.github/copilot-instructions.md`:

- **Location:** an `expect` in `commonMain`, or plain common code with header stubs on Android and the JVM.
- **Apple:** over the iOS SDK.
- **js and wasmJs:** over the JS SDK.
- **When a platform cannot provide it:** declare it anyway. That platform's actual does nothing, or throws where it must
  return a value, and is marked `@UnsupportedOnJs` (js and wasmJs) or `@UnsupportedOnApple` (Apple platforms). Only a
  signature that needs an Android-only type goes in `api/android-sdk/exclusions.txt`, with `@platform`. Never leave a
  member out silently.

Then run `./gradlew apiDump androidSourceCompatDump formatKotlin` and
`python3 .github/scripts/weekly/coverage.py --readme README.md`.

## Building on a laptop

- **Requirements:** JDK 17 or 21 (not 25), the Android SDK, and Xcode 26.2+ with the iOS platform. tvOS targets need
  the tvOS platform too (Xcode, Settings, Components).
- **Skipping tvOS:** to build without tvOS, override a module's targets, e.g.
  `-Pfirebase-auth.supportedTargets=ios,macos,jvm,js,wasmjs,android`.
- **Memory:** `gradle.properties` gives Gradle and the Kotlin daemon 8 GB each. On a 16 GB machine pass
  `-Dorg.gradle.jvmargs=-Xmx4g -Pkotlin.daemon.jvmargs=-Xmx3g`, and compile per target
  (`compileKotlinIosSimulatorArm64`, `compileKotlinWasmJs`, ...) rather than `assemble`.
- **Tests:** most tests need the Firebase emulators. Install `firebase-tools`, then run
  `firebase emulators:start --config=./test/firebase.json`. It starts the Firestore emulator in Enterprise edition, which
  runs pipelines (it still rejects BSON values, so `BsonTypesTest` stays ignored). CI runs the full matrix on every pull
  request.

## Releasing

1. Set `forkVersion` in `gradle.properties` (semantic versioning; `0.x` while upstream's `v3.0.0` is in alpha). Commit
   it to `main`.
2. Run the `Publish` workflow by hand on `main` (`gh workflow run publish.yml`). Run by hand it is a dry run: it builds
   every publication, tvOS included, and `.github/scripts/publish/check_publications.py` checks their POMs and Gradle
   module files. The `publications` artifact holds what would be uploaded. A version on Maven Central can never be
   replaced, so release only after this passes.
3. Create a GitHub release with the tag `v<forkVersion>`. The `Publish` workflow checks the tag, builds and checks every
   publication again, and uploads them all to the Central Portal as one deployment, which is released automatically.
   A failure anywhere uploads nothing. It then deploys the API docs to GitHub Pages.

### One-time Maven Central setup

1. Sign in to [central.sonatype.com](https://central.sonatype.com) with the `corneloaie` GitHub account. The namespace
   `io.github.corneloaie` is verified automatically for GitHub logins; if not, verify it under Namespaces.
2. Under Account, choose Generate User Token, and keep the page open.
3. Run `.github/scripts/publish/set-publishing-secrets.sh` in a terminal. It asks for the token's username and password,
   then for a passphrase. It creates a signing key with that passphrase, publishes the key's public half on
   keyserver.ubuntu.com, and stores the four secrets the Publish workflow reads (`SONATYPEUSERNAME`,
   `SONATYPEPASSWORD`, `GPG_PRIVATE_KEY`, `PASSPHRASE`). No value is printed, and none leaves your machine except to
   GitHub.

4. GitHub Pages serves the `gh-pages` branch, which the Docs workflow (`.github/workflows/docs.yml`) writes after each
   release; run that workflow by hand to refresh the API documentation from `main` without publishing.
