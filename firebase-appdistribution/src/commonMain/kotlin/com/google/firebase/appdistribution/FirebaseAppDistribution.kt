/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appdistribution

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase

/**
 * The Firebase App Distribution in-app tester API, mirroring `com.google.firebase.appdistribution.FirebaseAppDistribution`
 * from the Firebase Android SDK's API-only library (`firebase-appdistribution-api`): signs the tester in, checks for new
 * pre-release builds, updates the app to them and collects the tester's feedback.
 *
 * - **Android:** every call does nothing, and every task fails with
 *   [FirebaseAppDistributionException.Status.NOT_IMPLEMENTED], unless the app also depends on the full
 *   `com.google.firebase:firebase-appdistribution` SDK. Add it to pre-release build variants only: its self-update code may
 *   break Google Play policy.
 * - **iOS:** backed by the iOS SDK (`FirebaseAppDistribution-Beta`), which provides sign-in and the release check;
 *   [updateApp] and [updateIfNewReleaseAvailable] open the new release's download link, and iOS then asks the tester to
 *   install it. The iOS SDK has no in-app feedback, so the feedback members do nothing there and carry `@UnsupportedOnApple`.
 *   macOS and tvOS have no App Distribution SDK.
 * - **JVM:** behaves as Android does without the full SDK.
 */
public expect interface FirebaseAppDistribution {
    /**
     * Updates the app to the newest release, if one is available. If the tester is not signed in, asks them to sign in
     * first, then checks for a new release; the task succeeds when there is none. Otherwise, after the tester confirms,
     * an APK is downloaded and installed, and an AAB is handed to the Play app.
     *
     * On iOS, the iOS SDK's own alert asks a signed-out tester to sign in, and a new release's download link is opened
     * without an in-app confirmation: iOS asks the tester to install it. The task succeeds once the link is open, and
     * reports no progress.
     */
    public fun updateIfNewReleaseAvailable(): UpdateTask

    /** Whether the App Distribution tester is signed in. Always false on Android without the full SDK, and on the JVM. */
    public val isTesterSignedIn: Boolean

    /** Signs in the App Distribution tester, presenting a Google sign-in UI. */
    public fun signInTester(): Task<Nothing?>

    /** Signs out the App Distribution tester. */
    public fun signOutTester()

    /**
     * The new release available to the signed-in tester, or null if there is none; [updateApp] installs it. Fails with
     * [FirebaseAppDistributionException.Status.AUTHENTICATION_FAILURE] when the tester is not signed in. On iOS too:
     * there it does not show the sign-in alert that the iOS SDK's own `checkForUpdate` shows.
     */
    public fun checkForNewRelease(): Task<AppDistributionRelease?>

    /**
     * Updates the app to the release that the last [checkForNewRelease] returned: an APK is downloaded and installed, an
     * AAB is handed to the Play app. Fails with [FirebaseAppDistributionException.Status.UPDATE_NOT_AVAILABLE] when that
     * check found no new release.
     *
     * On iOS it opens the release's download link, checking for the release again if the link has expired (after 59
     * minutes); iOS then asks the tester to install it. The task succeeds once the link is open, and reports no progress.
     */
    public fun updateApp(): UpdateTask

    /**
     * Takes a screenshot, and starts an activity to collect and submit feedback from the tester. [additionalFormText]
     * is the string resource ID of a text shown to the tester before they submit it, such as a notice about how their
     * feedback is processed. Does nothing on iOS and the JVM, which have no in-app feedback.
     */
    public fun startFeedback(additionalFormText: Int)

    /** As [startFeedback] with a string resource ID, with the [additionalFormText] itself. */
    public fun startFeedback(additionalFormText: CharSequence)

    /**
     * Shows a notification that, when tapped, takes a screenshot of the current activity and starts the feedback
     * activity, see [startFeedback]. [additionalFormText] is a string resource ID; [interruptionLevel] is how much the
     * notification interrupts the tester. On Android 13 and above it needs the `POST_NOTIFICATIONS` permission. Does
     * nothing on iOS and the JVM.
     */
    public fun showFeedbackNotification(additionalFormText: Int, interruptionLevel: InterruptionLevel)

    /** As [showFeedbackNotification] with a string resource ID, with the [additionalFormText] itself. */
    public fun showFeedbackNotification(additionalFormText: CharSequence, interruptionLevel: InterruptionLevel)

    /** Hides the notification shown by [showFeedbackNotification]. Does nothing on iOS and the JVM. */
    public fun cancelFeedbackNotification()

    public companion object {
        /** The [FirebaseAppDistribution] singleton, which belongs to the default [com.google.firebase.FirebaseApp]. */
        public fun getInstance(): FirebaseAppDistribution
    }
}

/*
 * The Kotlin extensions of the Android SDK (FirebaseAppDistributionKt), as plain common code: on Android the facade is a
 * header stub that is stripped, so the SDK's own facade binds.
 */

/** The [FirebaseAppDistribution] singleton; the Android SDK's `Firebase.appDistribution`. */
public val Firebase.appDistribution: FirebaseAppDistribution
    get() = FirebaseAppDistribution.getInstance()

/** Destructures an [AppDistributionRelease]: its [AppDistributionRelease.binaryType]. */
public operator fun AppDistributionRelease.component1(): BinaryType = binaryType

/** Destructures an [AppDistributionRelease]: its [AppDistributionRelease.displayVersion]. */
public operator fun AppDistributionRelease.component2(): String = displayVersion

/** Destructures an [AppDistributionRelease]: its [AppDistributionRelease.versionCode]. */
public operator fun AppDistributionRelease.component3(): Long = versionCode

/** Destructures an [AppDistributionRelease]: its [AppDistributionRelease.releaseNotes]. */
public operator fun AppDistributionRelease.component4(): String? = releaseNotes

/** Destructures an [UpdateProgress]: its [UpdateProgress.apkBytesDownloaded]. */
public operator fun UpdateProgress.component1(): Long = apkBytesDownloaded

/** Destructures an [UpdateProgress]: its [UpdateProgress.apkFileTotalBytes]. */
public operator fun UpdateProgress.component2(): Long = apkFileTotalBytes

/** Destructures an [UpdateProgress]: its [UpdateProgress.updateStatus]. */
public operator fun UpdateProgress.component3(): UpdateStatus = updateStatus
