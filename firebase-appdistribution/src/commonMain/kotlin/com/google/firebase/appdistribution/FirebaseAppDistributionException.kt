/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appdistribution

import com.google.firebase.FirebaseException

/**
 * The exception that a failed [FirebaseAppDistribution] task fails with; the Android SDK's
 * `FirebaseAppDistributionException`. [errorCode] says why. The iOS SDK's errors map onto [Status.UNKNOWN],
 * [Status.AUTHENTICATION_FAILURE], [Status.AUTHENTICATION_CANCELED] and [Status.NETWORK_FAILURE].
 *
 * Plain common code: on Android and the JVM it has the SDK class's constructors, which the SDK hides from its reference
 * documentation.
 */
public open class FirebaseAppDistributionException : FirebaseException {
    /** Why the operation failed. */
    public val errorCode: Status

    /** The release that was ready to be installed when the error occurred, if any. */
    public val release: AppDistributionRelease?

    /** An exception with [message] for [status]. */
    public constructor(message: String, status: Status) : super(message) {
        errorCode = status
        this.release = null
    }

    /** An exception with [message] for [status], while [release] was ready to be installed. */
    public constructor(message: String, status: Status, release: AppDistributionRelease?) : super(message) {
        errorCode = status
        this.release = release
    }

    /** An exception with [message] for [status], caused by [cause]. */
    public constructor(message: String, status: Status, cause: Throwable) : super(message, cause) {
        errorCode = status
        this.release = null
    }

    /** An exception with [message] for [status], caused by [cause] while [release] was ready to be installed. */
    public constructor(message: String, status: Status, release: AppDistributionRelease?, cause: Throwable) : super(message, cause) {
        errorCode = status
        this.release = release
    }

    /** Why a [FirebaseAppDistribution] operation failed. In the Android SDK's order: `when` over an enum compiles to ordinals. */
    public enum class Status {
        /** An unknown error, or an error from another domain. */
        UNKNOWN,

        /**
         * The authentication failed: the tester was not signed in, has no access, or something went wrong. Signing in
         * again with [FirebaseAppDistribution.signInTester] may help.
         */
        AUTHENTICATION_FAILURE,

        /** The authentication was canceled, typically by the tester. */
        AUTHENTICATION_CANCELED,

        /** No network was available, or the request timed out. */
        NETWORK_FAILURE,

        /** The new release failed to download, most likely temporarily: retrying may help. */
        DOWNLOAD_FAILURE,

        /** The new release failed to install. Check that it has the same signing key as the installed app. */
        INSTALLATION_FAILURE,

        /** The installation was canceled, typically by the tester. */
        INSTALLATION_CANCELED,

        /**
         * No update was available for the tester and app: [FirebaseAppDistribution.updateApp] needs a release from
         * [FirebaseAppDistribution.checkForNewRelease].
         */
        UPDATE_NOT_AVAILABLE,

        /** The activity hosting a confirmation dialog was destroyed or sent to the back stack; call the method again. */
        HOST_ACTIVITY_INTERRUPTED,

        /**
         * Not implemented: on Android the build has the API-only library without the full
         * `com.google.firebase:firebase-appdistribution` SDK (intended for production builds), and the JVM has no App
         * Distribution.
         */
        NOT_IMPLEMENTED,

        /** The App Distribution Tester API is disabled for the project; enable it in the Google Cloud console. */
        API_DISABLED,
    }
}
