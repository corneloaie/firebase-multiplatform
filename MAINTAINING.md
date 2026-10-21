# Maintaining firebase-multiplatform

This fork of [GitLive's firebase-kotlin-sdk](https://github.com/GitLiveApp/firebase-kotlin-sdk) tracks upstream closely.
Everything that makes it a separate project is kept in a few places, so that the weekly upstream merges stay
mechanical:

| What | Where |
|---|---|
| Maven coordinates, version, POM | `buildSrc/src/main/kotlin/fork/Fork.kt`, `forkVersion` in `gradle.properties` |
| Upstream PRs merged ahead of upstream | `.github/upstream-merges.tsv` |
| Weekly automation | `.github/workflows/weekly.yml`, `.github/scripts/` |
| Publishing | `.github/workflows/publish.yml` |
| Docs | `README.md` (this fork's own, kept on upstream merges), `NOTICE`, this file |

Do not rename the Kotlin packages (`dev.gitlive.firebase.*`, `com.google.firebase.*`), or the Gradle `group`
(`dev.gitlive`). Renaming either turns every upstream merge into conflicts. The group also names the SwiftPM cinterop
packages (`swiftPMImport.dev.gitlive.firebase.<module>`) that every Apple source imports.

## Branches and remotes

- `main` is the default branch and what gets released.
- `upstream` is GitLive (`https://github.com/GitLiveApp/firebase-kotlin-sdk.git`). This fork follows its `v3.0.0` line,
  which uses SwiftPM, Kotlin 2.4 and Wasm, not its `master` (2.x, CocoaPods).
- `weekly/upstream-sync` and `weekly/firebase-sdks` are rewritten by the weekly workflow, one pull request each.

## The weekly workflow

It runs every Monday at 05:23 UTC, and on demand from the Actions tab (`Weekly update`, `Run workflow`).

### 1. Upstream merge (`.github/scripts/upstream/sync.sh`)

- Merges `upstream/v3.0.0`.
- For each line in `.github/upstream-merges.tsv` (branch, upstream PR number, merged head):
  - if the PR is merged into `v3.0.0` upstream, it drops the line;
  - if the branch head moved, it merges the new head and records it.
- Conflicts in `README.md` keep this fork's version.
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
| `androidSdk.apiRef` | `gradle.properties`, the newest `m<NNN> mergeback` commit of firebase-android-sdk, i.e. the API of the latest Firebase Android release |

It then runs `updateAndroidSdkApi` and `androidSourceCompatDump` and refreshes the README table. The pull request body
lists every member that became `MISS` or `OMIT`. A major version bump usually needs code changes; CI on the pull request
shows where.

### 3. Optional: Claude Code implements the gaps

Set the repository variable `CLAUDE_WEEKLY=true` and one of these secrets:

- `CLAUDE_CODE_OAUTH_TOKEN`, from `claude setup-token`, which bills a Claude subscription;
- `ANTHROPIC_API_KEY`, which bills the API.

The `implement` job then runs on a macOS runner after the SDK update. It makes every target compile, implements the
newly missing members, regenerates the reports and pushes to `weekly/firebase-sdks`. Review that pull request like any
other.

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
  `firebase emulators:start --config=./test/firebase.json`. CI runs the full matrix on every pull request.

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
2. Under Account, choose Generate User Token, then add the two values as repository secrets `SONATYPEUSERNAME` and
   `SONATYPEPASSWORD`.
3. Create a signing key and publish its public half:

   ```bash
   gpg --quick-generate-key "Cornel Oaie <your email>" rsa4096 sign 0
   gpg --list-secret-keys --keyid-format=long                      # note the key id
   gpg --keyserver keyserver.ubuntu.com --send-keys <KEYID>
   gpg --export-secret-keys --armor <KEYID> | gh secret set GPG_PRIVATE_KEY -R corneloaie/firebase-multiplatform
   gh secret set PASSPHRASE -R corneloaie/firebase-multiplatform   # the key's passphrase
   ```

4. Optionally, enable GitHub Pages for the `gh-pages` branch after the first release has created it.
