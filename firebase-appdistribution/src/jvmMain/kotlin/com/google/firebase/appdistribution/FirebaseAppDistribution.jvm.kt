/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appdistribution

import android.app.Activity
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCanceledListener
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import java.util.concurrent.Executor

/*
 * firebase-java-sdk has no App Distribution, so the JVM behaves as the Android SDK's API-only library does without the
 * full SDK (its FirebaseAppDistributionStub): the tester is never signed in, every task fails with NOT_IMPLEMENTED, and
 * the other members do nothing.
 */

public actual interface FirebaseAppDistribution {
    public actual fun updateIfNewReleaseAvailable(): UpdateTask
    public actual val isTesterSignedIn: Boolean
    public actual fun signInTester(): Task<Nothing?>
    public actual fun signOutTester()
    public actual fun checkForNewRelease(): Task<AppDistributionRelease?>
    public actual fun updateApp(): UpdateTask
    public actual fun startFeedback(additionalFormText: Int)
    public actual fun startFeedback(additionalFormText: CharSequence)
    public actual fun showFeedbackNotification(additionalFormText: Int, interruptionLevel: InterruptionLevel)
    public actual fun showFeedbackNotification(additionalFormText: CharSequence, interruptionLevel: InterruptionLevel)
    public actual fun cancelFeedbackNotification()

    public actual companion object {
        @JvmStatic
        public actual fun getInstance(): FirebaseAppDistribution = NotImplementedAppDistribution
    }
}

public actual abstract class UpdateTask actual constructor() : Task<Nothing?>() {
    public actual abstract fun addOnProgressListener(listener: OnProgressListener): UpdateTask
}

internal const val APP_DISTRIBUTION_UNSUPPORTED = "Firebase App Distribution is not available on the JVM"

internal fun <T> notImplemented(): Task<T> = TaskCompletionSource<T>().apply {
    setException(FirebaseAppDistributionException(APP_DISTRIBUTION_UNSUPPORTED, FirebaseAppDistributionException.Status.NOT_IMPLEMENTED))
}.task

private object NotImplementedAppDistribution : FirebaseAppDistribution {
    override fun updateIfNewReleaseAvailable(): UpdateTask = NotImplementedUpdateTask()

    override val isTesterSignedIn: Boolean get() = false

    override fun signInTester(): Task<Nothing?> = notImplemented()

    override fun signOutTester() {}

    override fun checkForNewRelease(): Task<AppDistributionRelease?> = notImplemented()

    override fun updateApp(): UpdateTask = NotImplementedUpdateTask()

    override fun startFeedback(additionalFormText: Int) {}

    override fun startFeedback(additionalFormText: CharSequence) {}

    override fun showFeedbackNotification(additionalFormText: Int, interruptionLevel: InterruptionLevel) {}

    override fun showFeedbackNotification(additionalFormText: CharSequence, interruptionLevel: InterruptionLevel) {}

    override fun cancelFeedbackNotification() {}

    override fun toString(): String = "FirebaseAppDistribution"
}

/**
 * A failed [UpdateTask] that never reports progress, as the Android SDK's NotImplementedUpdateTask: the Play Services
 * Task members delegate to a failed task (the listener-adding ones return this task, as the Android SDK's do).
 */
private class NotImplementedUpdateTask : UpdateTask() {
    private val task: Task<Nothing?> = notImplemented()

    override fun addOnProgressListener(listener: OnProgressListener): UpdateTask = this

    override fun isComplete(): Boolean = task.isComplete

    override fun isSuccessful(): Boolean = task.isSuccessful

    override fun isCanceled(): Boolean = task.isCanceled

    override fun getResult(): Nothing? = task.result

    override fun <X : Throwable> getResult(exceptionType: Class<X>): Nothing? = task.getResult(exceptionType)

    override fun getException(): Exception? = task.exception

    override fun addOnSuccessListener(listener: OnSuccessListener<in Nothing?>): Task<Nothing?> = apply { task.addOnSuccessListener(listener) }

    override fun addOnSuccessListener(executor: Executor, listener: OnSuccessListener<in Nothing?>): Task<Nothing?> = apply { task.addOnSuccessListener(executor, listener) }

    override fun addOnSuccessListener(activity: Activity, listener: OnSuccessListener<in Nothing?>): Task<Nothing?> = apply { task.addOnSuccessListener(activity, listener) }

    override fun addOnFailureListener(listener: OnFailureListener): Task<Nothing?> = apply { task.addOnFailureListener(listener) }

    override fun addOnFailureListener(executor: Executor, listener: OnFailureListener): Task<Nothing?> = apply { task.addOnFailureListener(executor, listener) }

    override fun addOnFailureListener(activity: Activity, listener: OnFailureListener): Task<Nothing?> = apply { task.addOnFailureListener(activity, listener) }

    override fun addOnCompleteListener(listener: OnCompleteListener<Nothing?>): Task<Nothing?> = apply { task.addOnCompleteListener { listener.onComplete(this) } }

    override fun addOnCompleteListener(executor: Executor, listener: OnCompleteListener<Nothing?>): Task<Nothing?> = apply { task.addOnCompleteListener(executor) { listener.onComplete(this) } }

    override fun addOnCompleteListener(activity: Activity, listener: OnCompleteListener<Nothing?>): Task<Nothing?> = apply { task.addOnCompleteListener(activity) { listener.onComplete(this) } }

    override fun addOnCanceledListener(listener: OnCanceledListener): Task<Nothing?> = apply { task.addOnCanceledListener(listener) }

    override fun addOnCanceledListener(executor: Executor, listener: OnCanceledListener): Task<Nothing?> = apply { task.addOnCanceledListener(executor, listener) }

    override fun addOnCanceledListener(activity: Activity, listener: OnCanceledListener): Task<Nothing?> = apply { task.addOnCanceledListener(activity, listener) }

    override fun <R> continueWith(continuation: Continuation<Nothing?, R>): Task<R> = task.continueWith { continuation.then(this) }

    override fun <R> continueWith(executor: Executor, continuation: Continuation<Nothing?, R>): Task<R> = task.continueWith(executor) { continuation.then(this) }

    override fun <R> continueWithTask(continuation: Continuation<Nothing?, Task<R>>): Task<R> = task.continueWithTask { continuation.then(this) }

    override fun <R> continueWithTask(executor: Executor, continuation: Continuation<Nothing?, Task<R>>): Task<R> = task.continueWithTask(executor) { continuation.then(this) }

    override fun <R> onSuccessTask(successContinuation: SuccessContinuation<Nothing?, R>): Task<R> = task.onSuccessTask(successContinuation)

    override fun <R> onSuccessTask(executor: Executor, successContinuation: SuccessContinuation<Nothing?, R>): Task<R> = task.onSuccessTask(executor, successContinuation)

    override fun toString(): String = "UpdateTask(failed: $APP_DISTRIBUTION_UNSUPPORTED)"
}
