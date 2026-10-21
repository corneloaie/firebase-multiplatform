/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appdistribution

import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCanceledListener
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.appdistribution.FirebaseAppDistributionException.Status
import dev.gitlive.firebase.UnsupportedOnApple
import platform.Foundation.NSError
import platform.UIKit.UIApplication
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import swiftPMImport.dev.gitlive.firebase.appdistribution.FIRAppDistribution
import swiftPMImport.dev.gitlive.firebase.appdistribution.FIRAppDistributionErrorDetailsKey
import swiftPMImport.dev.gitlive.firebase.appdistribution.FIRAppDistributionErrorDomain
import swiftPMImport.dev.gitlive.firebase.appdistribution.FIRAppDistributionRelease
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/*
 * App Distribution over the iOS SDK (FIRAppDistribution, a singleton of the default app). The iOS SDK signs the tester in
 * and checks for a new release; installing it means opening the release's download link, after which iOS asks the
 * tester to install the build, so the update tasks report no progress. It has no in-app feedback.
 */

public actual interface FirebaseAppDistribution {
    public actual fun updateIfNewReleaseAvailable(): UpdateTask
    public actual val isTesterSignedIn: Boolean
    public actual fun signInTester(): Task<Nothing?>
    public actual fun signOutTester()
    public actual fun checkForNewRelease(): Task<AppDistributionRelease?>
    public actual fun updateApp(): UpdateTask

    /** Does nothing: the iOS SDK has no in-app feedback. */
    @UnsupportedOnApple
    public actual fun startFeedback(additionalFormText: Int)

    /** Does nothing: the iOS SDK has no in-app feedback. */
    @UnsupportedOnApple
    public actual fun startFeedback(additionalFormText: CharSequence)

    /** Does nothing: the iOS SDK has no in-app feedback. */
    @UnsupportedOnApple
    public actual fun showFeedbackNotification(additionalFormText: Int, interruptionLevel: InterruptionLevel)

    /** Does nothing: the iOS SDK has no in-app feedback. */
    @UnsupportedOnApple
    public actual fun showFeedbackNotification(additionalFormText: CharSequence, interruptionLevel: InterruptionLevel)

    /** Does nothing: the iOS SDK has no in-app feedback. */
    @UnsupportedOnApple
    public actual fun cancelFeedbackNotification()

    public actual companion object {
        public actual fun getInstance(): FirebaseAppDistribution = IosFirebaseAppDistribution(FIRAppDistribution.appDistribution())
    }
}

public actual abstract class UpdateTask actual constructor() : Task<Nothing?>() {
    /** Never calls [listener]: the iOS SDK reports no download progress, as iOS installs a release itself. */
    @UnsupportedOnApple
    public actual abstract fun addOnProgressListener(listener: OnProgressListener): UpdateTask
}

private const val NOT_SIGNED_IN = "Tester is not signed in"

private const val RELEASE_NOT_FOUND = "Release not found. An update was not available for the current tester and app. " +
    "Make sure that checkForNewRelease() returns with a non-null AppDistributionRelease before calling updateApp()"

/** The release found by the last check, which updateApp installs, as the Android SDK keeps it; shared by every instance. */
@OptIn(ExperimentalAtomicApi::class)
private val cachedRelease = AtomicReference<FIRAppDistributionRelease?>(null)

/** The running updateIfNewReleaseAvailable, which a second call returns rather than prompting the tester again. */
@OptIn(ExperimentalAtomicApi::class)
private val runningUpdate = AtomicReference<IosUpdateTask?>(null)

/** The [FirebaseAppDistribution] of the iOS SDK's singleton [ios]. */
@OptIn(ExperimentalAtomicApi::class)
internal class IosFirebaseAppDistribution(val ios: FIRAppDistribution) : FirebaseAppDistribution {

    override val isTesterSignedIn: Boolean get() = ios.isTesterSignedIn

    override fun signInTester(): Task<Nothing?> {
        val source = TaskCompletionSource<Nothing?>()
        ios.signInTesterWithCompletion { error ->
            if (error == null) source.trySetResult(null) else source.trySetException(error.toAppDistributionException())
        }
        return source.task
    }

    override fun signOutTester() {
        ios.signOutTester()
        cachedRelease.store(null)
    }

    override fun checkForNewRelease(): Task<AppDistributionRelease?> {
        val source = TaskCompletionSource<AppDistributionRelease?>()
        checkSignedIn { release, e -> if (e == null) source.trySetResult(release?.let(::IosAppDistributionRelease)) else source.trySetException(e) }
        return source.task
    }

    override fun updateApp(): UpdateTask {
        val task = IosUpdateTask()
        val release = cachedRelease.load()
        when {
            !ios.isTesterSignedIn -> task.fail(FirebaseAppDistributionException(NOT_SIGNED_IN, Status.AUTHENTICATION_FAILURE))
            release == null -> task.fail(FirebaseAppDistributionException(RELEASE_NOT_FOUND, Status.UPDATE_NOT_AVAILABLE))
            // The download link expires 59 minutes after the check: check again for a fresh one.
            release.isExpired -> checkSignedIn { fresh, e ->
                when {
                    e != null -> task.fail(e)
                    fresh == null -> task.fail(FirebaseAppDistributionException(RELEASE_NOT_FOUND, Status.UPDATE_NOT_AVAILABLE))
                    else -> install(fresh, task)
                }
            }
            else -> install(release, task)
        }
        return task
    }

    override fun updateIfNewReleaseAvailable(): UpdateTask {
        runningUpdate.load()?.takeIf { !it.isComplete }?.let { return it }
        val task = IosUpdateTask()
        runningUpdate.store(task)
        // The iOS SDK's check asks a signed-out tester to sign in (its own alert), as the Android SDK's dialog does.
        checkForUpdate { release, e ->
            when {
                e != null -> task.fail(e)
                release == null -> task.succeed()
                else -> install(release, task)
            }
        }
        return task
    }

    /** Checks for a new release as the Android SDK does: failing, rather than prompting, when the tester is signed out. */
    private fun checkSignedIn(completion: (FIRAppDistributionRelease?, FirebaseAppDistributionException?) -> Unit) {
        if (ios.isTesterSignedIn) {
            checkForUpdate(completion)
        } else {
            completion(null, FirebaseAppDistributionException(NOT_SIGNED_IN, Status.AUTHENTICATION_FAILURE))
        }
    }

    private fun checkForUpdate(completion: (FIRAppDistributionRelease?, FirebaseAppDistributionException?) -> Unit) {
        ios.checkForUpdateWithCompletion { release, error ->
            val e = error?.toAppDistributionException()
            when {
                e == null -> cachedRelease.store(release)
                // The iOS SDK signs the tester out when it fails to authenticate them, which forgets the release too.
                e.errorCode == Status.AUTHENTICATION_FAILURE -> cachedRelease.store(null)
            }
            completion(release, e)
        }
    }

    /** Opens the download link of [release]; iOS then asks the tester to install the build. */
    private fun install(release: FIRAppDistributionRelease, task: IosUpdateTask) {
        dispatch_async(dispatch_get_main_queue()) {
            UIApplication.sharedApplication.openURL(release.downloadURL, emptyMap<Any?, Any?>()) { opened ->
                if (opened) {
                    task.succeed()
                } else {
                    task.fail(
                        FirebaseAppDistributionException(
                            "Could not open the download URL of the release",
                            Status.DOWNLOAD_FAILURE,
                            IosAppDistributionRelease(release),
                        ),
                    )
                }
            }
        }
    }

    @UnsupportedOnApple
    override fun startFeedback(additionalFormText: Int) {}

    @UnsupportedOnApple
    override fun startFeedback(additionalFormText: CharSequence) {}

    @UnsupportedOnApple
    override fun showFeedbackNotification(additionalFormText: Int, interruptionLevel: InterruptionLevel) {}

    @UnsupportedOnApple
    override fun showFeedbackNotification(additionalFormText: CharSequence, interruptionLevel: InterruptionLevel) {}

    @UnsupportedOnApple
    override fun cancelFeedbackNotification() {}

    override fun equals(other: Any?): Boolean = other is IosFirebaseAppDistribution && other.ios == ios

    override fun hashCode(): Int = ios.hashCode()

    override fun toString(): String = "FirebaseAppDistribution($ios)"
}

/** The [AppDistributionRelease] of the iOS SDK's [ios]. */
internal class IosAppDistributionRelease(val ios: FIRAppDistributionRelease) : AppDistributionRelease {
    override val displayVersion: String get() = ios.displayVersion
    override val versionCode: Long get() = ios.buildVersion.toLongOrNull() ?: -1
    override val releaseNotes: String? get() = ios.releaseNotes
    override val binaryType: BinaryType get() = BinaryType.APK

    override fun equals(other: Any?): Boolean = other is IosAppDistributionRelease && other.ios == ios

    override fun hashCode(): Int = ios.hashCode()

    override fun toString(): String = "AppDistributionRelease(displayVersion=$displayVersion, buildVersion=${ios.buildVersion}, releaseNotes=$releaseNotes)"
}

/** An [UpdateTask] completed by [succeed] or [fail]; its `Task` members delegate to a [TaskCompletionSource]'s task. */
internal class IosUpdateTask : UpdateTask() {
    private val source = TaskCompletionSource<Nothing?>()
    private val task: Task<Nothing?> get() = source.task

    fun succeed() {
        source.trySetResult(null)
    }

    fun fail(e: Exception) {
        source.trySetException(e)
    }

    override val isComplete: Boolean get() = task.isComplete
    override val isSuccessful: Boolean get() = task.isSuccessful
    override val isCanceled: Boolean get() = task.isCanceled
    override val result: Nothing? get() = task.result
    override val exception: Exception? get() = task.exception

    @UnsupportedOnApple
    override fun addOnProgressListener(listener: OnProgressListener): UpdateTask = this

    override fun addOnSuccessListener(listener: OnSuccessListener<Nothing?>): Task<Nothing?> = apply { task.addOnSuccessListener(listener) }

    override fun addOnFailureListener(listener: OnFailureListener): Task<Nothing?> = apply { task.addOnFailureListener(listener) }

    override fun addOnCompleteListener(listener: OnCompleteListener<Nothing?>): Task<Nothing?> = apply { task.addOnCompleteListener { listener.onComplete(this) } }

    override fun addOnCanceledListener(listener: OnCanceledListener): Task<Nothing?> = apply { task.addOnCanceledListener(listener) }

    override fun <TContinuationResult> continueWith(continuation: Continuation<Nothing?, TContinuationResult>): Task<TContinuationResult> = task.continueWith { continuation.then(this) }

    override fun <TContinuationResult> continueWithTask(continuation: Continuation<Nothing?, Task<TContinuationResult>>): Task<TContinuationResult> = task.continueWithTask { continuation.then(this) }

    override fun <TContinuationResult> onSuccessTask(successContinuation: SuccessContinuation<Nothing?, TContinuationResult>): Task<TContinuationResult> = task.onSuccessTask(successContinuation)

    override fun toString(): String = "UpdateTask(complete=$isComplete)"
}

// FIRAppDistributionError: Unknown = 0, AuthenticationFailure = 1, AuthenticationCancelled = 2, NetworkFailure = 3. The
// iOS SDK puts its message under FIRAppDistributionErrorDetailsKey rather than the localized description.
private fun NSError.toAppDistributionException(): FirebaseAppDistributionException = FirebaseAppDistributionException(
    userInfo[FIRAppDistributionErrorDetailsKey] as? String ?: localizedDescription,
    when {
        domain != FIRAppDistributionErrorDomain -> Status.UNKNOWN
        code == 1L -> Status.AUTHENTICATION_FAILURE
        code == 2L -> Status.AUTHENTICATION_CANCELED
        code == 3L -> Status.NETWORK_FAILURE
        else -> Status.UNKNOWN
    },
)
