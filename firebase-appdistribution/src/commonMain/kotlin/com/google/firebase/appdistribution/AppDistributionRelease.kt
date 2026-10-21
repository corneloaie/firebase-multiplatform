/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appdistribution

/**
 * A new release available to the signed-in tester, returned by [FirebaseAppDistribution.checkForNewRelease]; the Android
 * SDK's `AppDistributionRelease`. On iOS it wraps the iOS SDK's `FIRAppDistributionRelease`.
 *
 * On Android this is the SDK's Java interface, whose getters Kotlin cannot override with properties, so common code
 * cannot implement it.
 */
public interface AppDistributionRelease {
    /** The short version of the build, such as `1.0.0`. */
    public val displayVersion: String

    /**
     * The version code of the build, such as `123`. On iOS, the build version (`CFBundleVersion`) as a number, or -1 when
     * it is not a whole number (such as `1.0.1`).
     */
    public val versionCode: Long

    /** The release notes of the build, if any. */
    public val releaseNotes: String?

    /** The binary type of the build. Always [BinaryType.APK] on iOS, whose IPA builds are installed directly as an APK is. */
    public val binaryType: BinaryType
}

/** The binary type of an [AppDistributionRelease]; the Android SDK's `BinaryType`. */
public enum class BinaryType {
    /** An Android App Bundle, installed through the Play app. */
    AAB,

    /** An Android application package, downloaded and installed by the SDK. */
    APK,
}
