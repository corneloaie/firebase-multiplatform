# firebase-multiplatform

[![Android API coverage][coverage-badge]](#coverage)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.corneloaie/firebase-app?style=flat-square&label=Maven%20Central)](https://central.sonatype.com/namespace/io.github.corneloaie)
[![Weekly update](https://img.shields.io/github/actions/workflow/status/corneloaie/firebase-multiplatform/weekly.yml?label=weekly%20update&style=flat-square)](.github/workflows/weekly.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue?style=flat-square)](LICENSE)
[![GitHub Sponsors](https://img.shields.io/github/sponsors/corneloaie?label=sponsors&logo=githubsponsors&style=flat-square)](https://github.com/sponsors/corneloaie)
[![Ko-fi](https://img.shields.io/badge/Ko--fi-support-FF5E5B?style=flat-square&logo=ko-fi&logoColor=white)](https://ko-fi.com/corneloaie)

<!-- coverage-badge:start -->
[coverage-badge]: https://img.shields.io/badge/Android%20API%20coverage-100%25%3A%204%2C117%20of%204%2C117%20members%2C%2018%20products-brightgreen?style=flat-square
<!-- coverage-badge:end -->

**The complete Firebase Android SDK API in Kotlin Multiplatform common code**, for every Firebase client product, on
Android, iOS, macOS, tvOS, the JVM, JS and Wasm. It is built on each platform's official Firebase SDK and moved onto
their newest releases every week.

- **The Firebase Android SDK API in common code.** `com.google.firebase.*`, `Task` and listeners included, so an Android
  app's Firebase code moves into `commonMain` as it is.
- **The whole API, measured.** Every module is checked member by member against the Firebase Android SDK's API, and
  covers all of it; [the table below](#coverage) shows each one.
- **A Kotlin-first API next to it** (`io.github.corneloaie.firebase.*`), with suspending functions, `Flow` and
  kotlinx.serialization.
- **Published on Maven Central** as `io.github.corneloaie:<module>`.

## Coverage

<!-- coverage:start -->
**18 Firebase products: 4,117 of 4,117 Android API members available from common code (100%)**, measured against firebase-android-sdk [`5b34dd9`](https://github.com/firebase/firebase-android-sdk/tree/5b34dd98780b50f0f07d4381042ad35230d69b57). Built on the Firebase Android BoM 35.1.0 (App Distribution 16.0.0-beta21), iOS SDK 13.1.1 and JS SDK 13.1.0.

| Product | Artifact | Android API | Members | Android | iOS | macOS | tvOS | JVM | JS | Wasm |
|---|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| [AI Logic](https://firebase.google.com/docs/ai-logic) | `firebase-ai` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-ai/api/android-sdk-compat.txt) | 884 / 884 | ✅ | ✅ | ✅ | ✅ | – | ✅ | ✅ |
| [Analytics](https://firebase.google.com/docs/analytics) | `firebase-analytics` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-analytics/api/android-sdk-compat.txt) | 153 / 153 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| [App (core)](https://firebase.google.com/docs/reference/android/com/google/firebase/FirebaseApp) | `firebase-app` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-app/api/android-sdk-compat.txt) | 46 / 46 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| [App Check](https://firebase.google.com/docs/app-check) | `firebase-appcheck` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-appcheck/api/android-sdk-compat.txt) | 28 / 28 | ✅ | ✅ | ✅ | ✅ | – | ✅ | ✅ |
| [App Distribution](https://firebase.google.com/docs/app-distribution) | `firebase-appdistribution` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-appdistribution/api/android-sdk-compat.txt) | 62 / 62 | ✅ | ✅ | – | – | ✅ | – | – |
| [Authentication](https://firebase.google.com/docs/auth) | `firebase-auth` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-auth/api/android-sdk-compat.txt) | 284 / 284 | ✅ | ✅ | – | ✅ | ✅ | ✅ | ✅ |
| [Remote Config](https://firebase.google.com/docs/remote-config) | `firebase-config` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-config/api/android-sdk-compat.txt) | 92 / 92 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| [Crashlytics](https://firebase.google.com/docs/crashlytics) | `firebase-crashlytics` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-crashlytics/api/android-sdk-compat.txt) | 36 / 36 | ✅ | ✅ | ✅ | – | ✅ | – | – |
| [Realtime Database](https://firebase.google.com/docs/database) | `firebase-database` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-database/api/android-sdk-compat.txt) | 191 / 191 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| [Data Connect](https://firebase.google.com/docs/data-connect) | `firebase-dataconnect` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-dataconnect/api/android-sdk-compat.txt) | 197 / 197 | ✅ | ✅ | – | ✅ | – | ✅ | ✅ |
| [Cloud Firestore](https://firebase.google.com/docs/firestore) | `firebase-firestore` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-firestore/api/android-sdk-compat.txt) | 1667 / 1667 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| [Cloud Functions](https://firebase.google.com/docs/functions) | `firebase-functions` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-functions/api/android-sdk-compat.txt) | 60 / 60 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| [In-App Messaging](https://firebase.google.com/docs/in-app-messaging) | `firebase-inappmessaging` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-inappmessaging/api/android-sdk-compat.txt) | 76 / 76 | ✅ | ✅ | – | ✅ | ✅ | – | – |
| [Installations](https://firebase.google.com/docs/projects/manage-installations) | `firebase-installations` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-installations/api/android-sdk-compat.txt) | 11 / 11 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| [Cloud Messaging](https://firebase.google.com/docs/cloud-messaging) | `firebase-messaging` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-messaging/api/android-sdk-compat.txt) | 67 / 67 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| [Performance Monitoring](https://firebase.google.com/docs/perf-mon) | `firebase-perf` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-perf/api/android-sdk-compat.txt) | 50 / 50 | ✅ | ✅ | – | ✅ | ✅ | ✅ | ✅ |
| [Phone Number Verification](https://firebase.google.com/docs/phone-number-verification) | `firebase-pnv` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-pnv/api/android-sdk-compat.txt) | 47 / 47 | ✅ | ✅ | ✅ | ✅ | – | ✅ | ✅ |
| [Cloud Storage](https://firebase.google.com/docs/storage) | `firebase-storage` | [![100%](https://img.shields.io/badge/-100%25-brightgreen?style=flat-square)](firebase-storage/api/android-sdk-compat.txt) | 166 / 166 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
<!-- coverage:end -->

A module's coverage is the share of its Firebase Android SDK API that common code can call: the public, non-deprecated
members of its `api.txt`, or of the published classes for the closed-source Authentication, Analytics and Phone Number
Verification. The report behind each badge lists every member with one of these statuses:

| Status | Meaning | Counted |
|---|---|---|
| `OK` | identical signature | yes, as available |
| `MAP` | available with a mapped type (for example `java.util.Date` → `Timestamp`) | yes, as available |
| `MISS` | not provided yet | yes, as missing |
| `OMIT` | deliberately not mirrored | yes, as missing |
| `PLAT` | the signature needs an Android-only type (`Context`, `Activity`, `Uri`, ...) | no |
| `HIDE`, `SKIP`, `UNAV` | hidden in the Android SDK, deprecated, or not available yet | no |

**When a platform's SDK lacks a member**, it is still declared in common code. On that platform it does nothing, or
throws where it must return a value, and it carries an opt-in marker that warns at call sites compiled for that
platform: `@UnsupportedOnJs` (JS and Wasm) or `@UnsupportedOnApple` (iOS, macOS and tvOS).

**Platform notes:**
- **JVM.** The JVM target runs the [Firebase Java SDK](https://github.com/GitLiveApp/firebase-java-sdk), a port of the
  Android SDK. It compiles and is tested where upstream tests it, but it is not held to the same coverage as
  Android, iOS and the web.
- **Web.** Crashlytics, App Distribution and In-App Messaging have no web SDK.
- **Android only.** Phone Number Verification has no iOS, web or Java SDK. `firebase-pnv` compiles for Apple platforms,
  JS and Wasm so that common code using it builds there, but its operations fail with `UnsupportedOperationException`
  and carry `@UnsupportedOnApple` or `@UnsupportedOnJs`; only its value types, such as the token's claims, work there.
- **Not in the Java SDK.** It has no Data Connect or AI Logic and does not run App Check, so those three have no JVM
  target. It also lacks App Distribution and In-App Messaging; on the JVM those two act as the Android SDK does without
  its full library. App Distribution's tasks fail with `NOT_IMPLEMENTED`, and no in-app message is displayed. Its
  Firestore lacks vector values, the BSON types and the pipeline API, which JVM code cannot use.

### Not covered yet

- **AI Logic on-device inference** (`firebase-ai-ondevice`, a beta library outside the BoM whose API is deprecated in
  favor of `GenerativeModel.onDeviceExtension`): not mirrored yet. `firebase-ai` covers the rest of AI Logic.
- **ML Model Downloader**: not planned. Firebase removed it from iOS SDK 13 and Android BoM 35.

## Getting started

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
            implementation("io.github.corneloaie:firebase-firestore:0.1.2")
            implementation("io.github.corneloaie:firebase-auth:0.1.2")
        }
    }
}
```

Coming from GitLive's firebase-kotlin-sdk, or from version 0.1 of this one? Change the group to `io.github.corneloaie`
and the imports of `dev.gitlive.firebase.*` to `io.github.corneloaie.firebase.*`. The `com.google.firebase.*` packages
are unchanged.

To try a build of `main` before it is released, run `./gradlew publishToMavenLocal` in this repository and add
`mavenLocal()` to the repositories.

### Android

The minimum SDK is 24, as Firebase BoM 35 requires. Remote Config needs
[core library desugaring](https://developer.android.com/studio/write/java8-support#library-desugaring) below API 26.
Phone Number Verification needs compileSdk 35 or newer, which its androidx.core dependency requires.

### iOS, macOS and tvOS

The modules bind to the official [Firebase iOS SDK](https://github.com/firebase/firebase-ios-sdk) without bundling it:
each one records the `firebase-ios-sdk` Swift package it needs in its Maven publication, and your app inherits that
dependency. You do not add Firebase to Xcode yourself.

- **Requirements:** Kotlin 2.4 or newer and Xcode 26.2 or newer; Xcode 27 needs Kotlin 2.4.20
  ([KT-87196](https://youtrack.jetbrains.com/issue/KT-87196)). The minimum deployment targets are iOS 15, tvOS 15 and
  macOS 12.
- **Targets:** `iosArm64`, `iosX64`, `iosSimulatorArm64`, `macosArm64`, `tvosArm64` and `tvosSimulatorArm64`.
- **Integration:** use [direct integration](https://kotlinlang.org/docs/multiplatform/multiplatform-direct-integration.html)
  (the `embedAndSignAppleFrameworkForXcode` build phase). The Kotlin Gradle plugin then resolves the inherited
  dependency, generates the Swift package and links it, tests included.
- **More Firebase products, or a newer `firebase-ios-sdk`:** declare your own `swiftPMDependencies` in the shared module;
  the Kotlin Gradle plugin merges them with the inherited ones.
- **CocoaPods, or Firebase added to Xcode by hand:** the Kotlin Gradle plugin stops such builds with an error that
  prints the command switching your Xcode project to the generated package. See Kotlin's
  [CocoaPods to SwiftPM migration guide](https://kotl.in/cocoapods-to-swiftpm-migration), then remove the Firebase pods,
  as linking Firebase twice duplicates its symbols.

[documentation/ios-firebase-linking.md](documentation/ios-firebase-linking.md) explains how the dependency is passed on
and which products map to which Clang modules.

### JS and Wasm

Both targets bind to the official Firebase JS SDK, so they support the same modules and suit
[Compose Multiplatform for Web](https://www.jetbrains.com/lp/compose-multiplatform/).

### Initialization

`Firebase.initialize` is the entry point on every platform. Whether you need to call it, and what it reads, depends on
the platform:

| Platform | `context` argument | Without `options` | With `options` |
|---|---|---|---|
| Android | required, an `android.content.Context` | reads the resources the Google Services Gradle plugin generates from `google-services.json` | uses the given options |
| iOS, macOS, tvOS | ignored | reads `GoogleService-Info.plist` | uses the given options |
| JS and Wasm | ignored | not supported, throws `UnsupportedOperationException` | uses the given options (`authDomain` is JS and Wasm only) |
| JVM | required, an `android.app.Application` from the Firebase Java SDK | not supported | uses the given options |

On Android with the Google Services plugin, the official SDK initializes the default app at startup, so no call is
needed. On Apple platforms, configure the default app once at launch, either from Swift with `FirebaseApp.configure()`
or from Kotlin. On JS, Wasm and the JVM, the options are always passed in code. A common function covers every case:

```kotlin
// commonMain
fun initializeFirebase(context: Any? = null): FirebaseApp = Firebase.apps(context).firstOrNull() ?: Firebase.initialize(
    context,
    FirebaseOptions(
        applicationId = "1:1234567890:web:abc123",
        apiKey = "AIza...",
        projectId = "my-project",
        databaseUrl = "https://my-project.firebaseio.com",
        storageBucket = "my-project.appspot.com",
        gcmSenderId = "1234567890",
        authDomain = "my-project.firebaseapp.com", // JS and Wasm only
    ),
)
```

On the JVM, the [Firebase Java SDK needs its platform hooks](https://github.com/GitLiveApp/firebase-java-sdk?tab=readme-ov-file#initializing-the-sdk)
installed before the app is created, and its `Application` stand-in is the context:

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

`Firebase.initialize(context, options, name)` creates a further named app, and `Firebase.app(name)` returns it. Code
written against the Android SDK can keep calling `com.google.firebase.Firebase.initialize(context, options)`, which
follows the same rules.

## Usage

### The Firebase Android SDK API

The `com.google.firebase` packages are the Android SDK's own API, declared in common code and implemented on every
platform, so Android code compiles in `commonMain` without changing its imports:

```kotlin
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

Firebase.firestore.collection("cities").document("LA")
    .set(mapOf("name" to "Los Angeles", "state" to "CA"))
    .addOnSuccessListener { println("Saved") }
    .addOnFailureListener { e -> println("Failed: ${e.message}") }

val city = Firebase.firestore.collection("cities").document("LA").get().await()
```

[Migrating from the Firebase Android SDK](documentation/migrate-from-android.md) walks through moving Android code into
a shared module, including the compiler-guided replacement of the few Android-only members.

### The Kotlin-first API

The `io.github.corneloaie.firebase` packages wrap the same layer in suspending functions, `Flow` and kotlinx.serialization:

```kotlin
import io.github.corneloaie.firebase.Firebase
import io.github.corneloaie.firebase.firestore.firestore

@Serializable
data class City(val name: String, val state: String)

Firebase.firestore.collection("cities").document("LA").set(City("Los Angeles", "CA"))
Firebase.firestore.collection("cities").snapshots.collect { snapshot ->
    val cities = snapshot.documents.map { it.data<City>() }
}
```

[documentation/kotlin-first-api.md](documentation/kotlin-first-api.md) covers it in full: serialization settings,
server timestamps, field-by-field updates, the query DSL and more.

### Data Connect

The Data Connect Android SDK is already built on coroutines and kotlinx.serialization, so `firebase-dataconnect`
provides it as it is in common code, on Android, iOS, tvOS, JS and Wasm. Apple platforms go through a small Objective-C
wrapper package under `firebase-dataconnect/apple`, as the iOS SDK is Swift-only. On JS and Wasm,
`CacheSettings.Storage.PERSISTENT` behaves like `MEMORY`, and `DataConnectSettings.host` and `sslEnabled` are ignored.
Common code uses `kotlinx.datetime.LocalDate` with `KotlinxDatetimeLocalDateSerializer` and `kotlin.uuid.Uuid` with
`io.github.corneloaie.firebase.dataconnect.serializers.UuidSerializer`; the `java.time` and `java.util.UUID` variants are
Android-only.

### AI Logic

The AI Logic Android SDK is also built on coroutines, `Flow` and kotlinx.serialization, so `firebase-ai` provides it as
it is in common code, on Android, iOS, macOS, tvOS, JS and Wasm: the Gemini models with streaming, chats, structured
output (`generateObject` with a `JsonSchema`) and automatic function calling, server prompt templates and the Live API.
JS and Wasm run the JS SDK's `firebase/ai`; Apple platforms go through a small Objective-C wrapper package under
`firebase-ai/apple`, as the iOS SDK is Swift-only. Requests and responses cross to those SDKs as the JSON of the REST
API, and responses are checked and errors mapped as on Android. Common code sends images as `InlineDataPart`s (the
Android API's `Bitmap` overloads are Android-only). Off Android there is no on-device model (the models fall back to the
cloud, as Android does without the `firebase-ai-ondevice` library) and no audio conversation of the SDK's own
(`LiveSession.startAudioConversation`); see [the module's documentation](firebase-ai/documentation.md).

### Firestore Pipelines

`firebase-firestore` provides the pipeline API (Firestore Pipelines, a Firestore Enterprise edition feature):
`FirebaseFirestore.pipeline()`, the stages of `Pipeline`, and `com.google.firebase.firestore.pipeline` (`Expression`,
`AggregateFunction`, `Ordering`, the stage and option classes), on Android, iOS, macOS, tvOS, JS and Wasm. On Android
the Android SDK runs; elsewhere the pipeline is built in Kotlin and `execute()` hands it to the platform SDK: the JS
SDK's `firebase/firestore/pipelines`, and on Apple the Objective-C pipeline bridge that the iOS SDK's Swift API is built
on. A few options cannot be sent everywhere, and fail `execute()` with `UnsupportedOperationException`: on Apple,
execute options and source options other than the forced index (`CollectionHints.withForceIndex`); on JS and Wasm, a
field reference as an option other than a nearest-neighbour search's distance field or an unnest's index field. As for
document data, Kotlin/JS sends an integral double as an integer.

### The platform SDK objects

Every class has an `android`, `ios` or `js` property holding the official SDK's object, readable in that platform's
source set, for anything common code does not reach. The JVM uses `android` (the Java SDK is a port of the Android
SDK) and Wasm uses `js`:

```kotlin
// androidMain
Firebase.firestore.android.firestoreSettings = FirebaseFirestoreSettings.Builder(Firebase.firestore.android.firestoreSettings)
    .setPersistenceEnabled(false)
    .build()
```

## How it stays current

Every Monday the [weekly workflow](.github/workflows/weekly.yml) opens or refreshes the pull request "Weekly: Firebase
SDK updates", which runs the full CI matrix. It moves to the newest Firebase Android BoM, iOS SDK and JS SDK, and to
the Android API of the newest Firebase Android release. It regenerates the coverage reports and the table above, and
lists every new Android API member that is not available yet, and any new Firebase Android product no module covers.

A Claude Code routine then implements the new members on every platform, fixes whatever the new SDKs broke, and merges
the pull request once CI passes. A second routine keeps the other libraries, the build
tooling and the GitHub Actions current, in one more weekly pull request.
[MAINTAINING.md](MAINTAINING.md) has the details.

## Contributing

Issues and pull requests are welcome. To build locally:

1. Open the project in IntelliJ IDEA or Android Studio with JDK 17 or 21.
2. Install Xcode 26.2 or newer with the iOS platform, and the tvOS platform for the tvOS targets.
3. Build a target with, for example, `./gradlew compileKotlinIosSimulatorArm64`, or everything with
   `./gradlew publishToMavenLocal`.

Most tests need the Firebase emulators: run `firebase emulators:start` in the `test` directory (it starts Firestore in
Enterprise edition, which runs pipelines), then for example
`./gradlew jvmTest`, `./gradlew jsNodeTest` or `./gradlew iosSimulatorArm64Test`. Android instrumented tests run on a
Gradle-managed device in CI.

Before committing a change to the public API:
- run `./gradlew apiDump androidSourceCompatDump formatKotlin`;
- run `python3 .github/scripts/weekly/coverage.py --readme README.md`;
- document every public declaration in KDoc.

[MAINTAINING.md](MAINTAINING.md) covers the API rules, the weekly automation and releasing. API
documentation for each release is published [here](https://corneloaie.github.io/firebase-multiplatform/).

## Support

If firebase-multiplatform saves you time, you can support its upkeep through
[GitHub Sponsors](https://github.com/sponsors/corneloaie) or on [Ko-fi](https://ko-fi.com/corneloaie).

### Sponsors

Thank you to everyone who sponsors firebase-multiplatform. Monthly sponsors from $25 are listed here.

<!-- sponsors:start -->
_Be the first: [become a sponsor](https://github.com/sponsors/corneloaie)._
<!-- sponsors:end -->

## Acknowledgements

firebase-multiplatform started from [GitLive's firebase-kotlin-sdk](https://github.com/GitLiveApp/firebase-kotlin-sdk),
whose Kotlin-first API and Firebase Java SDK it keeps using. Thank you to GitLive and its contributors for that
foundation.

## License

Apache License 2.0, see [LICENSE](LICENSE) and [NOTICE](NOTICE).
