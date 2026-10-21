# firebase-multiplatform

[![License](https://img.shields.io/badge/license-Apache%202.0-blue?style=flat-square)](LICENSE)
[![Last commit](https://img.shields.io/github/last-commit/corneloaie/firebase-multiplatform?style=flat-square)](https://github.com/corneloaie/firebase-multiplatform/commits/main)
[![Weekly update](https://img.shields.io/github/actions/workflow/status/corneloaie/firebase-multiplatform/weekly.yml?label=weekly%20update&style=flat-square)](.github/workflows/weekly.yml)

**The Firebase Android SDK API from Kotlin Multiplatform common code**, on Android, iOS, macOS, tvOS, the JVM, JS and
Wasm, next to a Kotlin-first API built on coroutines, `Flow` and kotlinx.serialization. It is moved onto the newest
official Firebase SDKs every week.

Code written against the Firebase Android SDK (`com.google.firebase.*`, `Task`, listeners) compiles unchanged in
`commonMain`, so an Android app's Firebase code can move into a shared module as it is.

This is a fork of [GitLive's firebase-kotlin-sdk](https://github.com/GitLiveApp/firebase-kotlin-sdk) (Apache 2.0). See
[About this fork](#about-this-fork).

## Available libraries

<!-- coverage:start -->
| Product | Gradle dependency | Android API coverage | Members (identical / mapped / missing / omitted) |
|---|---|:---:|---|
| [AI Logic](https://firebase.google.com/docs/ai-logic) | `io.github.corneloaie:firebase-ai` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-ai/api/android-sdk-compat.txt) | 874: 874 / 0 / 0 / 0 |
| [Analytics](https://firebase.google.com/docs/analytics) | `io.github.corneloaie:firebase-analytics` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-analytics/api/android-sdk-compat.txt) | 153: 152 / 1 / 0 / 0 |
| [App (core)](https://firebase.google.com/docs/reference/android/com/google/firebase/FirebaseApp) | `io.github.corneloaie:firebase-app` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-app/api/android-sdk-compat.txt) | 46: 33 / 13 / 0 / 0 |
| [App Check](https://firebase.google.com/docs/app-check) | `io.github.corneloaie:firebase-appcheck` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-appcheck/api/android-sdk-compat.txt) | 28: 28 / 0 / 0 / 0 |
| [App Distribution](https://firebase.google.com/docs/app-distribution) | `io.github.corneloaie:firebase-appdistribution` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-appdistribution/api/android-sdk-compat.txt) | 62: 62 / 0 / 0 / 0 |
| [Authentication](https://firebase.google.com/docs/auth) | `io.github.corneloaie:firebase-auth` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-auth/api/android-sdk-compat.txt) | 284: 278 / 6 / 0 / 0 |
| [Remote Config](https://firebase.google.com/docs/remote-config) | `io.github.corneloaie:firebase-config` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-config/api/android-sdk-compat.txt) | 92: 91 / 1 / 0 / 0 |
| [Crashlytics](https://firebase.google.com/docs/crashlytics) | `io.github.corneloaie:firebase-crashlytics` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-crashlytics/api/android-sdk-compat.txt) | 36: 36 / 0 / 0 / 0 |
| [Realtime Database](https://firebase.google.com/docs/database) | `io.github.corneloaie:firebase-database` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-database/api/android-sdk-compat.txt) | 191: 191 / 0 / 0 / 0 |
| [Data Connect](https://firebase.google.com/docs/data-connect) | `io.github.corneloaie:firebase-dataconnect` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-dataconnect/api/android-sdk-compat.txt) | 197: 197 / 0 / 0 / 0 |
| [Cloud Firestore](https://firebase.google.com/docs/firestore) | `io.github.corneloaie:firebase-firestore` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-firestore/api/android-sdk-compat.txt) | 364: 352 / 12 / 0 / 0 |
| [Cloud Functions](https://firebase.google.com/docs/functions) | `io.github.corneloaie:firebase-functions` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-functions/api/android-sdk-compat.txt) | 60: 54 / 6 / 0 / 0 |
| [In-App Messaging](https://firebase.google.com/docs/in-app-messaging) | `io.github.corneloaie:firebase-inappmessaging` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-inappmessaging/api/android-sdk-compat.txt) | 76: 76 / 0 / 0 / 0 |
| [Installations](https://firebase.google.com/docs/projects/manage-installations) | `io.github.corneloaie:firebase-installations` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-installations/api/android-sdk-compat.txt) | 11: 11 / 0 / 0 / 0 |
| [Cloud Messaging](https://firebase.google.com/docs/cloud-messaging) | `io.github.corneloaie:firebase-messaging` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-messaging/api/android-sdk-compat.txt) | 67: 67 / 0 / 0 / 0 |
| [Performance Monitoring](https://firebase.google.com/docs/perf-mon) | `io.github.corneloaie:firebase-perf` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-perf/api/android-sdk-compat.txt) | 50: 49 / 1 / 0 / 0 |
| [Cloud Storage](https://firebase.google.com/docs/storage) | `io.github.corneloaie:firebase-storage` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-storage/api/android-sdk-compat.txt) | 166: 161 / 5 / 0 / 0 |
<!-- coverage:end -->

**Android API coverage** is the share of the Firebase Android SDK's public, non-deprecated API (its `api.txt`, or the
published classes for the closed-source Authentication and Analytics) that common code can use through the
`com.google.firebase` layer. Each module's [`api/android-sdk-compat.txt`](firebase-firestore/api/android-sdk-compat.txt)
report lists every member and its status, regenerated with `./gradlew :<module>:androidSourceCompatDump`:

| Status | Meaning | Counted |
|---|---|---|
| `OK` | identical signature | yes, as available |
| `MAP` | available with a mapped type (for example `java.util.Date` → `Timestamp`) | yes, as available |
| `MISS` | not provided yet | yes, as missing |
| `OMIT` | deliberately not mirrored | yes, as missing |
| `PLAT` | the signature needs an Android/JVM-only type (`Context`, `Activity`, `Uri`, ...) | no |
| `HIDE`, `SKIP`, `UNAV` | hidden in the Android SDK, or not applicable | no |

The reports are measured against the firebase-android-sdk revision in `androidSdk.apiRef` (`gradle.properties`). The
weekly update moves it to the newest Firebase Android release.

### Platforms

| Module | Android | iOS | macOS | tvOS | JVM | JS | Wasm |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `firebase-ai` | ✅ | ✅ | ✅ | ✅ | – | ✅ | ✅ |
| `firebase-analytics` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `firebase-app` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `firebase-appcheck` | ✅ | ✅ | ✅ | ✅ | – | ✅ | ✅ |
| `firebase-appdistribution` | ✅ | ✅ | – | – | ✅ | – | – |
| `firebase-auth` | ✅ | ✅ | – | ✅ | ✅ | ✅ | ✅ |
| `firebase-config` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `firebase-crashlytics` | ✅ | ✅ | ✅ | – | ✅ | – | – |
| `firebase-database` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `firebase-dataconnect` | ✅ | ✅ | – | ✅ | – | ✅ | ✅ |
| `firebase-firestore` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `firebase-functions` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `firebase-inappmessaging` | ✅ | ✅ | – | ✅ | ✅ | – | – |
| `firebase-installations` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `firebase-messaging` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `firebase-perf` | ✅ | ✅ | – | ✅ | ✅ | ✅ | ✅ |
| `firebase-storage` | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

The JVM target runs GitLive's [Firebase Java SDK](https://github.com/GitLiveApp/firebase-java-sdk), a port of the
Android SDK. It compiles and is tested where upstream tests it, but it is not held to the same coverage as Android, iOS
and the web. Crashlytics, App Distribution and In-App Messaging have no web SDK, the Java SDK does not run App Check,
and Data Connect and AI Logic have no Java implementation. firebase-java-sdk has neither App Distribution nor In-App
Messaging, so on the JVM those two compile and act as the Android SDK does without its full library: App Distribution's
tasks fail with `NOT_IMPLEMENTED`, and no in-app message is ever displayed.

### Gradle

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// build.gradle.kts of the shared module
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.github.corneloaie:firebase-firestore:0.1.0")
            implementation("io.github.corneloaie:firebase-auth:0.1.0")
        }
    }
}
```

The Kotlin packages are GitLive's (`dev.gitlive.firebase.*` and `com.google.firebase.*`), so a project using GitLive's
SDK switches by changing the group from `dev.gitlive` to `io.github.corneloaie`. To use a build of `main` before it is
released, run `./gradlew publishToMavenLocal` here and add `mavenLocal()` to the repositories.

## About this fork

GitLive's SDK is excellent, and its maintainers are moving every module onto the `com.google.firebase` source
compatibility layer that gives the coverage above. This fork follows them closely and adds the following on top:

- **GitLive's in-progress work, merged early.** The source-compatibility pull requests for every module, plus Data
  Connect, are merged on top of GitLive's `v3.0.0` line ahead of upstream. These are
  [#881](https://github.com/GitLiveApp/firebase-kotlin-sdk/pull/881) to
  [#891](https://github.com/GitLiveApp/firebase-kotlin-sdk/pull/891) and
  [#915](https://github.com/GitLiveApp/firebase-kotlin-sdk/pull/915). The exact heads are tracked in
  [`.github/upstream-merges.tsv`](.github/upstream-merges.tsv).
- **Wasm for all of it.** Those pull requests predate `v3.0.0`'s Wasm target, so this fork implements their
  `com.google.firebase` layer for `wasmJs` too.
- **Fixes found on the way.** The header-stub verifier now reads Kotlin metadata to tell private-constructor accessors
  from inline value class constructors. The layer is adapted to the iOS SDK 12 API changes (Storage metadata references,
  the removed Dynamic Links domain). The JS Storage `getInstance(url)` no longer passes a `null` app. Fixes that apply
  upstream go back to GitLive as pull requests.
- **Kept current every week.** See [Staying current](#staying-current).
- **Published as `io.github.corneloaie:<module>`** on Maven Central, so GitLive's own `dev.gitlive` artifacts are left
  alone.

## Staying current

Every Monday the [weekly workflow](.github/workflows/weekly.yml) opens (or refreshes) two pull requests, each of which
runs the full CI matrix:

1. **Weekly: merge upstream GitLive.** It merges GitLive's `v3.0.0` and the new commits of the upstream pull requests
   this fork carries, and drops each one once GitLive has merged it. Conflicts it cannot resolve mechanically are
   reported in an issue instead.
2. **Weekly: Firebase SDK updates.** It moves to the newest Firebase Android BoM, iOS SDK and JS SDK, and to the
   Android API of the newest Firebase Android release. Then it regenerates the coverage reports and this README's
   table, and lists every Android API member that is new and not yet available.

Optionally, a Claude Code job then implements those members on every platform and fixes whatever the new SDKs broke,
pushing to the second pull request (see [MAINTAINING.md](MAINTAINING.md)). Dependabot keeps the build tooling and
GitHub Actions up to date.

## Not covered yet

The coverage table counts every member of each module's Android API except the ones its report marks `PLAT` (the
signature needs an Android/JVM-only type) or `UNAV`/`HIDE`/`SKIP`. Larger surfaces outside it so far:

- **Firestore Pipelines** (`com.google.firebase.firestore.pipeline`, about 1,300 Android members, an Enterprise
  edition preview): excluded as unavailable while the iOS and JS SDKs lacked it. The JS SDK 13 now ships
  `firebase/firestore/pipelines`.
- **AI Logic on-device inference** (`firebase-ai-ondevice`, a beta library outside the BoM whose API is deprecated in
  favor of `GenerativeModel.onDeviceExtension`): not mirrored yet. `firebase-ai` covers the rest of AI Logic.
- **Phone Number Verification** (`firebase-pnv`): Android-only and closed source; no module yet.
- **ML Model Downloader** is not planned: Firebase removed it from the iOS SDK 13 and from the Android BoM 35.

## Multiplatform

The Firebase Kotlin SDK provides a common API to access Firebase for projects targeting *iOS*, *Android*, *JVM*, *JS* and *Wasm (wasmJs)* meaning you can use Firebase directly in your common code. Under the hood, the SDK achieves this by binding to the respective official Firebase SDK for each supported platform.

It uses the <a href="https://github.com/GitLiveApp/firebase-java-sdk">Firebase Java SDK</a> to support the JVM target. The library requires [additional initialization](https://github.com/GitLiveApp/firebase-java-sdk?tab=readme-ov-file#initializing-the-sdk) compared to the official Firebase SDKs.

Both the *JS* and *Wasm (wasmJs)* targets bind to the official Firebase JS SDK, so they support the same set of modules and are well suited to [Compose Multiplatform for Web](https://www.jetbrains.com/lp/compose-multiplatform/).

### Initialization

`Firebase.initialize` is the one entry point for every platform, but what it does, and whether you need to call it
at all, depends on how the underlying Firebase SDK is initialised:

| Platform | `context` argument | Without `options` | With `options` |
|----------|--------------------|-------------------|----------------|
| Android | required, an `android.content.Context` | reads the resources generated by the Google Services Gradle plugin from `google-services.json` | uses the given options |
| iOS | ignored | reads `GoogleService-Info.plist` (`FirebaseApp.configure()`) | uses the given options |
| JS and Wasm | ignored | not supported, throws `UnsupportedOperationException` | uses the given options (`authDomain` is JS and Wasm only) |
| JVM | required, an `android.app.Application` from the [Firebase Java SDK](https://github.com/GitLiveApp/firebase-java-sdk) | not supported | uses the given options |

On Android you usually need no call: with the Google Services Gradle plugin the official SDK initialises the default app itself
at startup (through a `ContentProvider` merged into your manifest) and `Firebase.app` is ready to use. On iOS the default app has
to be configured once at launch, either from Swift with `FirebaseApp.configure()` or from Kotlin as shown below, before any Firebase
API is used. On JS, Wasm and the JVM the options always have to be provided in code.

The recommended pattern is a common function that takes the options and an optional platform context, called from each
platform's entry point:

```kotlin
// commonMain
fun initializeFirebase(context: Any? = null): FirebaseApp = Firebase.apps(context).firstOrNull() ?: Firebase.initialize(
    context,
    FirebaseOptions(
        applicationId = "1:846484016111:web:abc123",
        apiKey = "AIza...",
        projectId = "fir-kotlin-sdk",
        databaseUrl = "https://fir-kotlin-sdk.firebaseio.com",
        storageBucket = "fir-kotlin-sdk.appspot.com",
        gcmSenderId = "846484016111",
        authDomain = "fir-kotlin-sdk.firebaseapp.com", // JS and Wasm only, ignored elsewhere
    ),
)
```

```kotlin
// androidMain, e.g. in Application.onCreate; only needed without the Google Services plugin
initializeFirebase(applicationContext)

// iosMain, e.g. from the app delegate or the SwiftUI App init; or pass no options to read GoogleService-Info.plist
initializeFirebase()

// jsMain and wasmJsMain, before any other Firebase call
initializeFirebase()
```

On the JVM the [Firebase Java SDK needs its platform hooks installed](https://github.com/GitLiveApp/firebase-java-sdk?tab=readme-ov-file#initializing-the-sdk)
before the app is created, and the `context` is the SDK's `Application` stand-in:

```kotlin
// jvmMain
FirebasePlatform.initializeFirebasePlatform(object : FirebasePlatform() {
    val storage = mutableMapOf<String, String>()
    override fun store(key: String, value: String) = storage.set(key, value)
    override fun retrieve(key: String) = storage[key]
    override fun clear(key: String) { storage.remove(key) }
    override fun log(msg: String) = println(msg)
})
initializeFirebase(Application())
```

`Firebase.initialize(context, options, name)` initialises an additional named app and `Firebase.app(name)` returns it;
`Firebase.apps(context)` lists the initialised apps and `FirebaseApp.delete()` removes one. The default app is named
`FirebaseApp.DEFAULT_APP_NAME` on every platform, even though the underlying SDKs use different names for it.

Code written against the Android SDK can keep using `com.google.firebase.Firebase.initialize(context, options)`: the same
three overloads exist there with the context typed as `Any?`, following the rules above per platform.



### Using the Firebase Android SDK API from common code

Alongside the Kotlin-first `dev.gitlive.firebase` API, the SDK ships the Firebase Android SDK API itself under its original
`com.google.firebase` packages, as `expect` declarations implemented on every platform. Code written against the Android SDK
(including `com.google.android.gms.tasks.Task` and its listeners) therefore compiles unchanged, without even changing imports,
in common code targeting Android, iOS, JVM, JS and Wasm (wasmJs):

```kotlin
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.tasks.await

FirebaseInstallations.getInstance().getId()
    .addOnSuccessListener { id -> println("Installation id: $id") }
    .addOnFailureListener { e -> println("Failed: ${e.message}") }

val token = FirebaseInstallations.getInstance().getToken(false).await().token
```

Every module ships this layer; the [table above](#available-libraries) shows how much of each Android SDK it covers. For
a step-by-step walkthrough of moving Android SDK code into a shared module, including the compiler-guided replacement
of the Android-only members, see [Migrating from the Firebase Android SDK](documentation/migrate-from-android.md).

`firebase-dataconnect` provides the whole `com.google.firebase.dataconnect` API of the Firebase Data Connect Android SDK
(the one Firebase SDK built on kotlinx.serialization: `query(operationName, variables, dataDeserializer, variablesSerializer)`,
`OptionalVariable`, `EnumValue`, `LocalDate`, `AnyValue` and the `serializers` package) on Android, iOS, tvOS, JS and Wasm. On
Android the Android SDK runs; on JS and Wasm the Firebase JS SDK's `firebase/data-connect` (`CacheSettings.Storage.PERSISTENT`
behaves like `MEMORY`, and `DataConnectSettings.host`/`sslEnabled` are ignored, as the JS SDK has no such settings); on
Apple the Firebase iOS Data Connect SDK, through the small Objective-C wrapper package under `firebase-dataconnect/apple`
that the Kotlin SwiftPM import builds (the SDK is Swift-only). There is no JVM target, as the Firebase Java SDK has no
Data Connect. The
`java.time.LocalDate` conversions, `JavaTimeLocalDateSerializer` and `UUIDSerializer` exist on Android only; common code
uses `kotlinx.datetime.LocalDate` with `KotlinxDatetimeLocalDateSerializer` and `kotlin.uuid.Uuid` with
`dev.gitlive.firebase.dataconnect.serializers.UuidSerializer`, which writes the same 32 hexadecimal digits.

`firebase-ai` provides the `com.google.firebase.ai` API of the Firebase AI Logic Android SDK, which is built on
coroutines, `Flow` and kotlinx.serialization like Data Connect's, on Android, iOS, macOS, tvOS, JS and Wasm: the Gemini
models with streaming, chats, structured output (`generateObject` with a `JsonSchema`) and automatic function calling,
server prompt templates and the Live API. On Android the Android SDK runs; on JS and Wasm the Firebase JS SDK's
`firebase/ai`; on Apple the Firebase AI Logic iOS SDK, through the Objective-C wrapper package under
`firebase-ai/apple` (the SDK is Swift-only). Requests and responses cross to the JS and iOS SDKs as the JSON of the REST
API, and responses are checked and errors mapped as on Android. Images are `android.graphics.Bitmap`s in parts of the
Android API; common code sends them as `InlineDataPart`s. There is no on-device model off Android (the models fall back
to the cloud, as Android does without the `firebase-ai-ondevice` library), and the Live API's audio conversation, which
records and plays audio itself, is Android-only. See [the module's documentation](firebase-ai/documentation.md).

### Accessing the underlying Firebase SDK

In some cases you might want to access the underlying official Firebase SDK in platform specific code, for example when the common API is missing the functionality you need. For this purpose each class in the SDK has `android`, `ios` and `js` extension properties that hold the equivalent object of the underlying official Firebase SDK. For *JVM*, as the `firebase-java-sdk` is a direct port of the Firebase Android SDK, is it also accessed via the `android` property. The *Wasm (wasmJs)* target binds to the same official Firebase JS SDK as the *JS* target and exposes it via the `js` property.

These properties are only accessible from the equivalent target's source set. For example to disable persistence in Cloud Firestore on Android you can write the following in your Android specific code (e.g. `androidMain` or `androidTest`):

```kotlin
  Firebase.firestore.android.firestoreSettings = FirebaseFirestoreSettings.Builder(Firebase.firestore.android.firestoreSettings)
          .setPersistenceEnabled(false)
          .build()
```

### Running on Android

On android, some modules (`config`) require you to enable [Core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring) if you have a *minSDK lower than API 26*.

### Running on iOS

On iOS the SDK binds to the official [Firebase iOS SDK](https://github.com/firebase/firebase-ios-sdk). It is **not bundled** into the published klibs — instead, each module records the `firebase-ios-sdk` Swift Package Manager dependency it needs as metadata in its Maven publication, so **your KMP app inherits it transitively**. You do not declare or link Firebase yourself.

Requires a minimum deployment target of **iOS 15 / tvOS 15 / macOS 12**. iOS and tvOS come from `firebase-ios-sdk` 13; macOS 12 is the minimum Kotlin/Native builds for. The Apple targets are `iosArm64`, `iosX64`, `iosSimulatorArm64`, `macosArm64`, `tvosArm64` and `tvosSimulatorArm64`. There are no Intel `macosX64` or `tvosX64` targets, which Kotlin deprecated in 2.3.20 and removes in 2.5.20; apps that still build them need the 2.x releases.

**Recommended (Swift Package Manager, automatic).** Consume the SDK via Maven as usual (e.g. `implementation("io.github.corneloaie:firebase-auth:<version>")`) and integrate your shared framework into Xcode using [direct integration](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html) (the `embedAndSignAppleFrameworkForXcode` build phase). The Kotlin Gradle plugin resolves the inherited `firebase-ios-sdk` dependency, generates the Swift package, and links it for you (including for iOS/macOS/tvOS tests). Requires **Kotlin 2.4+** and **Xcode 26.2+**; Xcode 27 needs Kotlin 2.4.20+ in your build ([KT-87196](https://youtrack.jetbrains.com/issue/KT-87196)). Note: the [remote / SwiftPM-export](https://kotlinlang.org/docs/multiplatform/multiplatform-spm-export.html) method (packaging your shared module as its own Swift package) needs Kotlin 2.4.20+ ([KT-84420](https://youtrack.jetbrains.com/issue/KT-84420)), is experimental, and hasn't been verified end to end with this SDK; see [documentation/ios-firebase-linking.md](documentation/ios-firebase-linking.md).

To add Firebase products this SDK doesn't use, or to require a newer `firebase-ios-sdk`, declare your own `swiftPMDependencies` in your shared module. The Kotlin Gradle plugin merges them with the inherited ones. The first argument is the package URL, and `from(...)` accepts that version up to the next major:

```kotlin
kotlin {
    swiftPMDependencies {
        swiftPackage(
            url = url("https://github.com/firebase/firebase-ios-sdk.git"),
            version = from("13.0.1"),
            products = listOf(product("FirebaseAppCheck")),
        )
    }
}
```

**CocoaPods, or linking Firebase yourself.** Because each module publishes its `firebase-ios-sdk` dependency, the Kotlin Gradle plugin fails the two integrations in which you added Firebase to Xcode yourself:

- The Kotlin CocoaPods plugin: `syncFramework` fails in `checkSwiftPMDependencies` with "You are using CocoaPods integration with SwiftPM dependencies".
- Direct integration when your Xcode project doesn't include the generated linkage package: the build fails with "SwiftPM linkage package not integrated into Xcode project".

Both errors print the command that switches your Xcode project over (the `integrateEmbedAndSign` and `integrateLinkagePackage` tasks); see Kotlin's [CocoaPods to SwiftPM migration guide](https://kotl.in/cocoapods-to-swiftpm-migration). Then remove the Firebase pods from your Podfile, because linking Firebase through both CocoaPods and SwiftPM duplicates its symbols.

See [documentation/ios-firebase-linking.md](documentation/ios-firebase-linking.md) for how the transitive metadata works and the product → Clang-module mapping.

## Kotlin-first design

Unlike the Kotlin Extensions for the Firebase Android SDK this project does not extend a Java based SDK so we get the full power of Kotlin including coroutines and serialization!

<h3><a href="https://kotlinlang.org/docs/tutorials/coroutines/async-programming.html#coroutines">Suspending functions</a></h3>

Asynchronous operations that return a single or no value are represented by suspending functions in the SDK instead of callbacks, listeners or OS specific types such as [Task](https://developer.android.com/reference/com/google/android/play/core/tasks/Task), for example:

```kotlin
suspend fun signInWithCustomToken(token: String): AuthResult
```

It is important to remember that unlike a callback based API, waiting for suspending functions to complete is implicit and so if you don't want to wait for the result you can `launch` a new coroutine:

```kotlin
//TODO don't use GlobalScope
GlobalScope.launch {
  Firebase.auth.signOut()
}
```

<h3><a href="https://kotlinlang.org/docs/reference/coroutines/flow.html">Flows</a></h3>

Asynchronous streams of values are represented by Flows in the SDK instead of repeatedly invoked callbacks or listeners, for example:

```kotlin
val snapshots: Flow<DocumentSnapshot>
```

The flows are cold, which means a new listener is added every time a terminal operator is applied to the resulting flow. A buffer with the [default size](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/-b-u-f-f-e-r-e-d.html) is used to buffer values received from the listener, use the [`buffer` operator](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/buffer.html) on the flow to specify a user-defined value and to control what happens when data is produced faster than consumed, i.e. to control the back-pressure behavior. Often you are only interested in the latest value received, in this case you can use the [`conflate` operator](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/conflate.html) to disable buffering.

The listener is removed once the flow [completes](https://kotlinlang.org/docs/reference/coroutines/flow.html#flow-completion) or is [cancelled](https://kotlinlang.org/docs/reference/coroutines/flow.html#flow-cancellation).

<h3><a href="https://github.com/Kotlin/kotlinx.serialization">Serialization</a></h3>

The official Firebase SDKs use different platform-specific ways to support writing data with and without custom classes in [Cloud Firestore](https://firebase.google.com/docs/firestore/manage-data/add-data#custom_objects), [Realtime Database](https://firebase.google.com/docs/database/android/read-and-write#basic_write) and [Functions](https://firebase.google.com/docs/functions/callable).

The Firebase Kotlin SDK uses Kotlin serialization to read and write custom classes to Firebase. To use Kotlin serialization in your project add the plugin to your gradle file:

```groovy
plugins {
    kotlin("multiplatform") version "1.9.20" // or kotlin("jvm") or any other kotlin plugin
    kotlin("plugin.serialization") version "1.9.20"
}
```

Then mark your custom classes `@Serializable`:

```kotlin
@Serializable
data class City(val name: String)
```

Instances of these classes can now be passed [along with their serializer](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#introduction-to-serializers) to the SDK:

```kotlin
db.collection("cities").document("LA").set(City.serializer(), city) { encodeDefaults = true }
```

The `buildSettings` closure is optional and allows for configuring serialization behaviour. 

Setting the `encodeDefaults` parameter is optional and defaults to `true`, set this to false to omit writing optional properties if they are equal to theirs default values.
Using [@EncodeDefault](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-encode-default/) on properties is a recommended way to locally override the behavior set with `encodeDefaults`.

You can also omit the serializer if it can be inferred using `serializer<KType>()`. 
To support [contextual serialization](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#contextual-serialization) or [open polymorphism](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/polymorphism.md#open-polymorphism) the `serializersModule` can be overridden in the `buildSettings` closure:

```kotlin
@Serializable
abstract class AbstractCity {
    abstract val name: String
}

@Serializable
@SerialName("capital")
data class Capital(override val name: String, val isSeatOfGovernment: Boolean) : AbstractCity()

val module = SerializersModule {
    polymorphic(AbstractCity::class, AbstractCity.serializer()) {
        subclass(Capital::class, Capital.serializer())
    }
}

val city = Capital("London", true)
db.collection("cities").document("UK").set(AbstractCity.serializer(), city) { 
    encodeDefaults = true
    serializersModule = module
    
}
val storedCity = db.collection("cities").document("UK").get().data(AbstractCity.serializer()) {
    serializersModule = module
}
```

<h4><a href="https://firebase.google.com/docs/firestore/manage-data/add-data#server_timestamp">Server Timestamp</a></h3>

[Firestore](https://firebase.google.com/docs/reference/kotlin/com/google/firebase/firestore/FieldValue?hl=en#serverTimestamp()) and the [Realtime Database](https://firebase.google.com/docs/reference/android/com/google/firebase/database/ServerValue#TIMESTAMP) provide a sentinel value you can use to set a field in your document to a server timestamp. So you can use these values in custom classes:

```kotlin
@Serializable
data class Post(
    // In case using Realtime Database.
    val timestamp = ServerValue.TIMESTAMP,
    // In case using Cloud Firestore.
    val timestamp: Timestamp = Timestamp.ServerTimestamp,
    // or
    val alternativeTimestamp = FieldValue.serverTimestamp,
    // or
    @Serializable(with = DoubleAsTimestampSerializer::class),
    val doubleTimestamp: Double = DoubleAsTimestampSerializer.serverTimestamp
)
```

In addition `firebase-firestore` provides [GeoPoint] and [DocumentReference] classes which allow persisting
geo points and document references in a native way:

```kotlin
@Serializable
data class PointOfInterest(
    val reference: DocumentReference, 
    val location: GeoPoint
)
val document = PointOfInterest(
    reference = Firebase.firestore.collection("foo").document("bar"),
    location = GeoPoint(51.939, 4.506)
)
```

<h4>Polymorphic serialization (sealed classes)</h4>

This sdk will handle polymorphic serialization automatically if you have a sealed class and its children marked as `Serializable`. It will include a `type` property that will be used to discriminate which child class is the serialized.

You can change this `type` property by using the `@FirebaseClassDiscrminator` annotation in the parent sealed class:

```kotlin
@Serializable
@FirebaseClassDiscriminator("class")
sealed class Parent {
    @Serializable
    @SerialName("child")
    data class Child(
        val property: Boolean
    ) : Parent
}
```

In combination with a `SerialName` specified for the child class, you have full control over the serialized data. In this case it will be:

```json
{
  "class": "child",
  "property": true
}
```

<h4>Serialization of Updates</h4>
Firestore contains update methods that allow for multiple fields to be updated at the same time. 
This sdk offers special update methods that allow for applying custom serialization to each individual field though an update builder.
Where an `update` method exists, an `updateFields` method will also be available. In this, each value can have its serializer customized:

```kotlin
documentRef.updateFields {
    // Root level encode settings
    encodeDefaults = false
    serializersModule = module
    
    "field" to "value"
    // Set the value of otherField to "1" using a custom Serializer
    "otherField".to(IntAsStringSerializer(), 1)
    
    // Overwrite build settings. All fields added within this block will have these build settings applied
    withEncodeSettings {
        encodeDefaults = true
        serializersModule = otherModule
        "city" to abstractCity
    }
}
```

Similarly, the `Query` methods `startAt`/`startAfter`/`endAt`/`endBefore` have an alternative method in `startAtFieldValues`/`startAfterFieldValues`/`endAtFieldValues`/`endBeforeFieldValues`

```kotlin
query.orderBy("field", "otherField", "city").startAtFieldValues { // similar syntax for startAfter/endAt/endBefore
    // Root level encode settings
    encodeDefaults = false
    serializersModule = module
    
    add("Value")

    // Starts at "1" for the otherField value
    add(1, IntAsStringSerializer())

    // Overwrite build settings. All field values added within this block will have these build settings applied
    withEncodeSettings {
        encodeDefaults = true
        serializersModule = otherModule
        add(abstractCity)
    }
}
```

<h3><a href="https://kotlinlang.org/docs/reference/functions.html#default-arguments">Default arguments</a></h3>

To reduce boilerplate, default arguments are used in the places where the Firebase Android SDK employs the builder pattern:
```kotlin
UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
        .setDisplayName("Jane Q. User")
        .setPhotoUri(Uri.parse("https://example.com/jane-q-user/profile.jpg"))
        .build()

user.updateProfile(profileUpdates)
        .addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "User profile updated.")
                }
            }
        })

//...becomes...

user.updateProfile(displayName = "Jane Q. User", photoURL = "https://example.com/jane-q-user/profile.jpg")
```

<h3><a href="https://kotlinlang.org/docs/functions.html#infix-notation">Infix notation</a></h3>

To improve readability and reduce boilerplate for functions such as the Cloud Firestore query operators are built with infix notation:

```kotlin
citiesRef.whereEqualTo("state", "CA")
citiesRef.whereArrayContains("regions", "west_coast")
citiesRef.where(Filter.and(
    Filter.equalTo("state", "CA"),
    Filter.or(
        Filter.equalTo("capital", true),
        Filter.greaterThanOrEqualTo("population", 1000000)
    )
))

//...becomes...

citiesRef.where { "state" equalTo "CA" }
citiesRef.where { "regions" contains "west_coast" }
citiesRef.where {
    all(
        "state" equalTo "CA",
        any(
            "capital" equalTo true,
            "population" greaterThanOrEqualTo 1000000
        )
    )
}
```

<h3><a href="https://kotlinlang.org/docs/reference/operator-overloading.html">Operator overloading</a></h3>

In cases where it makes sense, such as Firebase Functions HTTPS Callable, operator overloading is used:

```kotlin
    val addMessage = functions.getHttpsCallable("addMessage")
    //In the official android Firebase SDK this would be addMessage.call(...)
    addMessage(mapOf("text" to text, "push" to true))
```

## Contributing
Issues and pull requests are welcome; changes that apply to GitLive's SDK are best made there first, as this fork merges
upstream every week. To build and test locally:
1. Open the project in IntelliJ IDEA or Android Studio, with JDK 17 or 21.
2. Install Xcode 26.2+ with the iOS (and, for the tvOS targets, tvOS) platform. The Apple targets use Kotlin's Swift
   Package Manager integration, which requires Kotlin 2.4+; CocoaPods is not needed.
3. After a Gradle sync, run `publishToMavenLocal`.

[MAINTAINING.md](MAINTAINING.md) covers the weekly automation, the upstream merges and releasing.

### Testing
To run the tests you can use the following gradle tasks:

`./gradlew connectedAndroidTest` (an emulator needs to be running)

`./gradlew iosSimulatorArm64Test` (On Apple Chip) `./gradlew iosX64Test` (On Intel Chip)

`./gradlew jsNodeTest`

`./gradlew jvmTest`

For some tests you need to have the firebase emulator suite running. After installing them you can run the following command inside the `test` directory:

`firebase emulators:start`

### Documentation
For every publicly available class or function there needs to be documentation written in the [KDoc syntax](https://kotlinlang.org/docs/kotlin-doc.html).
A new version of the documentation is published with every release [here](https://corneloaie.github.io/firebase-multiplatform/docs/).

### Validation
To ensure changes to the public API are well documented, this library validates its binary API. If you make changes to the API, make sure to run

`./gradlew apiDump`

### Code style
This library uses the [Intellij Kotlin code style](https://www.jetbrains.com/help/idea/code-style-kotlin.html). Run the linter to make sure you follow these styles.

`./gradlew formatKotlin` to format to the proper style
`./gradlew lintKotlin` to validate the correct style is used

### Compatibility with the official [Firebase Android SDK](https://github.com/firebase/firebase-android-sdk)

When this project began, the official Firebase Android SDK was a pure java library and the separate Kotlin extensions (KTX) module consisted of only a few extensions providing some syntactic sugar, for example `Firebase.firestore` instead of `Firebase.getInstance(),` which we naturally copied for the Firebase Kotlin SDK.

But with the majority of the Android SDK being designed for Java, the Kotlin SDK was primarily guided by our [Kotlin-first design](https://github.com/GitLiveApp/firebase-kotlin-sdk/?tab=readme-ov-file#kotlin-first-design) principles.

More recently, with the official SDK for Android providing better support for Kotlin and the inclusion of the new Kotlin-friendly features direct in the main modules, the API differences between the official SDK and this project are likely to start to blur. Therefore, in particular for developers porting android code to multiplatform, one of our goals going forward will be API compatibility with the Android SDK where possible.

For contributors this means following these points when adding new code to the public API of this project:
- **Match the [Android SDKs API](https://firebase.google.com/docs/reference/kotlin/packages).** When adding new API coverage use the Android SDK as the guide on what the public API should be in regard to naming, parameters etc. The goal here is *source compatibility*: the `com.google.firebase` layer (see [Using the Firebase Android SDK API from common code](#using-the-firebase-android-sdk-api-from-common-code)) takes the exact Android shape so code consuming the Android SDK compiles *as is*, and the `dev.gitlive` layer keeps the same names with Kotlin-first shapes. Run `./gradlew :<module>:androidSourceCompatDump` to see which Android SDK members a module still lacks.
- **Follow our [Kotlin-first design](https://github.com/GitLiveApp/firebase-kotlin-sdk/?tab=readme-ov-file#kotlin-first-design) principles when needed.** If the API you are adding coverage for is new, and it's Kotlin-first in the Android SDK, then you can simply just match the Android SDKs API as described in the first point, but if it's an older Java-first API then ideally we would include an identical API for API compatibility *plus* a Kotlin-first overload. A good example for this is where the Builder pattern is employed in the Android SDK, here we can follow [this Kotlin-first design principle](https://github.com/GitLiveApp/firebase-kotlin-sdk/?tab=readme-ov-file#default-arguments) and provide both methods, one taking the options created with the builder and an overload with default arguments to avoid the builder boilerplate for developers not porting an existing android code base.

And finally, please remember that this is an open source project, all the project maintainers are **volunteers**, they are **not paid to maintain** this project, and they have **their own jobs**, so please be **patient** when waiting for a response to your issue or PR. Any form of abuse or harassment will not be tolerated and will result in being reported to GitHub.
