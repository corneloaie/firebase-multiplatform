/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appdistribution

import android.net.Uri
import com.google.android.gms.tasks.Task
import io.github.corneloaie.firebase.appdistribution.stub

/*
 * Header stubs for com.google.firebase:firebase-appdistribution-api (see buildSrc utils/HeaderStubs.kt): compiled
 * against, verified to match the real classes, and deleted from the output so the real SDK binds at runtime. UpdateTask
 * extends the real Play Services Task (firebase-app strips its stubs), so it leaves the Task members inherited. The
 * screenshot members take an android.net.Uri and are not in the common API; they are declared here so that the shipped
 * String extensions (FeedbackScreenshot.android.kt) bind to them.
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

    /** The SDK's Uri member; the shipped String extension of common code binds to it. */
    public fun startFeedback(additionalFormText: Int, screenshotUri: Uri?)

    /** The SDK's Uri member; the shipped String extension of common code binds to it. */
    public fun startFeedback(additionalFormText: CharSequence, screenshotUri: Uri?)
    public actual fun showFeedbackNotification(additionalFormText: Int, interruptionLevel: InterruptionLevel)
    public actual fun showFeedbackNotification(additionalFormText: CharSequence, interruptionLevel: InterruptionLevel)
    public actual fun cancelFeedbackNotification()

    public actual companion object {
        @JvmStatic
        public actual fun getInstance(): FirebaseAppDistribution = stub()
    }
}

public actual abstract class UpdateTask actual constructor() : Task<Nothing?>() {
    public actual abstract fun addOnProgressListener(listener: OnProgressListener): UpdateTask
}
