// swift-tools-version:5.9

// Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.

// The Firebase AI Logic iOS SDK (FirebaseAILogic) is Swift-only, which Kotlin/Native cannot import. This package wraps
// it in an Objective-C API (see Sources), the workaround JetBrains recommends for Swift-only packages with the Kotlin
// SwiftPM import; firebase-ai/build.gradle.kts imports it with localSwiftPackage(). FirebaseAILogic is a product of
// firebase-ios-sdk, so the Kotlin SwiftPM lock file keeps it at the version the other modules use.

import PackageDescription

let package = Package(
  name: "FirebaseAILogicObjC",
  platforms: [.iOS(.v15), .macOS(.v12), .tvOS(.v15)],
  products: [
    .library(name: "FirebaseAILogicObjC", targets: ["FirebaseAILogicObjC"]),
  ],
  dependencies: [
    // The version is the firebase-ios-sdk version of gradle/libs.versions.toml.
    .package(url: "https://github.com/firebase/firebase-ios-sdk.git", from: "13.0.1"),
  ],
  targets: [
    .target(
      name: "FirebaseAILogicObjC",
      dependencies: [
        .product(name: "FirebaseAILogic", package: "firebase-ios-sdk"),
        // FirebaseCore for FirebaseApp.app(name:).
        .product(name: "FirebaseCore", package: "firebase-ios-sdk"),
      ],
      path: "Sources"
    ),
  ]
)
