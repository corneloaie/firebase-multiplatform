/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("AppDistributionKt")
@file:JvmMultifileClass

package dev.gitlive.firebase.appdistribution

import com.google.firebase.appdistribution.UpdateTask
import com.google.firebase.appdistribution.startFeedback
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.UnsupportedOnApple
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import com.google.firebase.appdistribution.FirebaseAppDistribution as CompatFirebaseAppDistribution
import com.google.firebase.appdistribution.appDistribution as compatAppDistribution

// The Android-SDK-shaped singleton is reached through the `Firebase.appDistribution` extension (FirebaseAppDistributionKt),
// a real static method on every platform, rather than the companion object of the header stub.

/** Returns the [FirebaseAppDistribution] instance of the default app. */
public val Firebase.appDistribution: FirebaseAppDistribution
    get() = FirebaseAppDistribution(com.google.firebase.Firebase.compatAppDistribution)

/**
 * Firebase App Distribution's in-app tester API: signs the tester in, checks for new pre-release builds, updates the app
 * to them and collects the tester's feedback.
 *
 * On Android it does nothing (and fails with [AppDistributionExceptionStatus.NOT_IMPLEMENTED]) unless the app's
 * pre-release builds add the full `com.google.firebase:firebase-appdistribution` SDK. On iOS an update opens the new
 * release's download link, and there is no in-app feedback. The JVM has no App Distribution. See
 * [com.google.firebase.appdistribution.FirebaseAppDistribution] for each member's behaviour per platform.
 *
 * @property compat The Android-SDK-shaped [com.google.firebase.appdistribution.FirebaseAppDistribution] this wraps.
 */
public class FirebaseAppDistribution internal constructor(public val compat: CompatFirebaseAppDistribution) {
    /** Whether the App Distribution tester is signed in. */
    public val isTesterSignedIn: Boolean get() = compat.isTesterSignedIn

    /** Signs in the App Distribution tester, presenting a Google sign-in UI; throws a [FirebaseAppDistributionException] if that fails. */
    public suspend fun signInTester() {
        compat.signInTester().await()
    }

    /** Signs out the App Distribution tester. */
    public fun signOutTester() {
        compat.signOutTester()
    }

    /**
     * The new release available to the signed-in tester, or null if there is none; [updateApp] installs it. Throws a
     * [FirebaseAppDistributionException] with [AppDistributionExceptionStatus.AUTHENTICATION_FAILURE] when the
     * tester is not signed in.
     */
    public suspend fun checkForNewRelease(): AppDistributionRelease? = compat.checkForNewRelease().await()

    /**
     * Updates the app to the release that the last [checkForNewRelease] returned. Collecting the flow starts the update;
     * it emits the update's progress, completes when the update has, and throws the [FirebaseAppDistributionException]
     * the update failed with ([AppDistributionExceptionStatus.UPDATE_NOT_AVAILABLE] without a new release).
     * Cancelling the collection does not cancel the update. On iOS it emits nothing: the release's download link is
     * opened, and iOS asks the tester to install it.
     */
    public fun updateApp(): Flow<UpdateProgress> = updates { compat.updateApp() }

    /**
     * Updates the app to the newest release, if one is available, asking a signed-out tester to sign in first; see
     * [updateApp] for the flow. It completes without an update when there is no new release.
     */
    public fun updateIfNewReleaseAvailable(): Flow<UpdateProgress> = updates { compat.updateIfNewReleaseAvailable() }

    /**
     * Starts an activity to collect and submit feedback from the tester, with [additionalFormText] shown to them before
     * they submit it. The feedback comes with the screenshot at [screenshotUri] (a `content://` or `file://` URI of a
     * bitmap), or with one the SDK takes of the current activity when it is null. Does nothing on iOS and the JVM.
     */
    @OptIn(UnsupportedOnApple::class)
    public fun startFeedback(additionalFormText: CharSequence, screenshotUri: String? = null) {
        if (screenshotUri == null) compat.startFeedback(additionalFormText) else compat.startFeedback(additionalFormText, screenshotUri)
    }

    /**
     * Shows a notification that starts the feedback of [startFeedback] when the tester taps it, interrupting them as
     * much as [interruptionLevel]. On Android 13 and above it needs the `POST_NOTIFICATIONS` permission. Does nothing
     * on iOS and the JVM.
     */
    @OptIn(UnsupportedOnApple::class)
    public fun showFeedbackNotification(additionalFormText: CharSequence, interruptionLevel: InterruptionLevel = InterruptionLevel.DEFAULT) {
        compat.showFeedbackNotification(additionalFormText, interruptionLevel)
    }

    /** Hides the notification shown by [showFeedbackNotification]. Does nothing on iOS and the JVM. */
    @OptIn(UnsupportedOnApple::class)
    public fun cancelFeedbackNotification() {
        compat.cancelFeedbackNotification()
    }

    override fun equals(other: Any?): Boolean = other is FirebaseAppDistribution && other.compat == compat

    override fun hashCode(): Int = compat.hashCode()

    override fun toString(): String = "FirebaseAppDistribution($compat)"
}

/** The progress of [start]'s update, as a cold flow that fails with the update's exception; no progress is dropped. */
@OptIn(UnsupportedOnApple::class)
private fun updates(start: () -> UpdateTask): Flow<UpdateProgress> = callbackFlow {
    val task = start()
    task.addOnProgressListener { trySend(it) }
    task.addOnCompleteListener { completed -> close(completed.exception) }
    // An UpdateTask cannot be cancelled, nor its listeners removed: a cancelled collection just stops receiving.
    awaitClose()
}.buffer(Channel.UNLIMITED)

/** A new release, returned by [FirebaseAppDistribution.checkForNewRelease]. */
public typealias AppDistributionRelease = com.google.firebase.appdistribution.AppDistributionRelease

/** The binary type of an [AppDistributionRelease]. */
public typealias BinaryType = com.google.firebase.appdistribution.BinaryType

/** The progress, or final state, of an update. */
public typealias UpdateProgress = com.google.firebase.appdistribution.UpdateProgress

/** The state of an update, in an [UpdateProgress]. */
public typealias UpdateStatus = com.google.firebase.appdistribution.UpdateStatus

/** How much the notification of [FirebaseAppDistribution.showFeedbackNotification] interrupts the tester. */
public typealias InterruptionLevel = com.google.firebase.appdistribution.InterruptionLevel

/** The exception that a failed App Distribution operation throws; its `errorCode`, an [AppDistributionExceptionStatus], says why. */
public typealias FirebaseAppDistributionException = com.google.firebase.appdistribution.FirebaseAppDistributionException

/** Why an App Distribution operation failed: the `errorCode` of a [FirebaseAppDistributionException]. */
public typealias AppDistributionExceptionStatus = com.google.firebase.appdistribution.FirebaseAppDistributionException.Status
