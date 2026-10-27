/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.appdistribution

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.appdistribution.AppDistributionRelease
import com.google.firebase.appdistribution.BinaryType
import com.google.firebase.appdistribution.FirebaseAppDistribution
import com.google.firebase.appdistribution.FirebaseAppDistributionException
import com.google.firebase.appdistribution.FirebaseAppDistributionException.Status
import com.google.firebase.appdistribution.InterruptionLevel
import com.google.firebase.appdistribution.OnProgressListener
import com.google.firebase.appdistribution.UpdateProgress
import com.google.firebase.appdistribution.UpdateStatus
import com.google.firebase.appdistribution.UpdateTask
import com.google.firebase.appdistribution.appDistribution
import com.google.firebase.appdistribution.component1
import com.google.firebase.appdistribution.component2
import com.google.firebase.appdistribution.component3
import com.google.firebase.appdistribution.component4
import com.google.firebase.appdistribution.startFeedback
import io.github.corneloaie.firebase.UnsupportedOnApple
import io.github.corneloaie.firebase.runTest
import kotlinx.coroutines.tasks.await
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Exercises the `com.google.firebase.appdistribution` layer exactly as Android app code would (static accessor, Task
 * API, listeners, the Kotlin extensions), on every platform. No tester is signed in and, on Android, the build has the
 * API-only library, so the operations fail: with NOT_IMPLEMENTED on Android and the JVM, and with AUTHENTICATION_FAILURE
 * (rather than a sign-in prompt) on iOS. Members that present UI are only compiled, in [compileOnly].
 */
@IgnoreForAndroidUnitTest
@OptIn(UnsupportedOnApple::class)
class AndroidSdkSourceCompatTest {

    @BeforeTest
    fun initialize() = initializeFirebase()

    @Test
    fun testInstance() {
        val appDistribution: FirebaseAppDistribution = FirebaseAppDistribution.getInstance()
        assertEquals(appDistribution, Firebase.appDistribution)
        assertFalse(appDistribution.isTesterSignedIn)
        appDistribution.signOutTester()
        assertFalse(appDistribution.isTesterSignedIn)
    }

    @Test
    fun testCheckForNewReleaseWithoutTester() = runTest {
        val task: Task<AppDistributionRelease?> = FirebaseAppDistribution.getInstance().checkForNewRelease()
        val e = assertFailsWith<FirebaseAppDistributionException> { task.await() }
        assertTrue(e.errorCode == Status.NOT_IMPLEMENTED || e.errorCode == Status.AUTHENTICATION_FAILURE, "${e.errorCode}")
        assertNull(e.release)
        assertTrue(task.isComplete)
        assertFalse(task.isSuccessful)
    }

    @Test
    fun testUpdateAppWithoutRelease() = runTest {
        val progress = mutableListOf<UpdateStatus>()
        val task: UpdateTask = FirebaseAppDistribution.getInstance().updateApp()
            .addOnProgressListener(OnProgressListener { progress += it.updateStatus })
        val e = assertFailsWith<FirebaseAppDistributionException> { task.await() }
        assertTrue(e.errorCode == Status.NOT_IMPLEMENTED || e.errorCode == Status.AUTHENTICATION_FAILURE, "${e.errorCode}")
        assertEquals(e.errorCode, assertIs<FirebaseAppDistributionException>(task.exception).errorCode)
        assertFalse(task.isSuccessful)
        assertFalse(task.isCanceled)
        assertTrue(progress.isEmpty())
        val chained = task.continueWith { it.exception?.message }.await()
        assertEquals(e.message, chained)
    }

    @Test
    fun testFeedbackWithoutFullSdk() {
        // Without the full SDK (Android), and on iOS and the JVM, these do nothing.
        val appDistribution = FirebaseAppDistribution.getInstance()
        appDistribution.startFeedback("Feedback")
        appDistribution.startFeedback("Feedback", "content://io.github.corneloaie.firebase/screenshot.png")
        appDistribution.showFeedbackNotification("Feedback", InterruptionLevel.HIGH)
        appDistribution.cancelFeedbackNotification()
    }

    @Test
    fun testException() {
        val e = FirebaseAppDistributionException("Tester is not signed in", Status.AUTHENTICATION_FAILURE)
        assertIs<FirebaseException>(e)
        assertEquals(Status.AUTHENTICATION_FAILURE, e.errorCode)
        assertEquals("Tester is not signed in", e.message)
        assertNull(e.release)
        val cause = IllegalStateException("cause")
        val withCause = FirebaseAppDistributionException("Download failed", Status.DOWNLOAD_FAILURE, cause)
        assertSame(cause, withCause.cause)
        assertEquals(Status.DOWNLOAD_FAILURE, withCause.errorCode)
        assertNull(FirebaseAppDistributionException("No release", Status.UPDATE_NOT_AVAILABLE, null as AppDistributionRelease?).release)
    }

    @Test
    fun testEnums() {
        // The Android SDK's order: `when` over these compiles to ordinals.
        assertEquals(listOf("AAB", "APK"), BinaryType.entries.map { it.name })
        assertEquals(listOf("MIN", "LOW", "DEFAULT", "HIGH", "MAX"), InterruptionLevel.entries.map { it.name })
        assertEquals(
            listOf(
                "PENDING", "DOWNLOADING", "DOWNLOADED", "DOWNLOAD_FAILED", "INSTALL_CANCELED", "INSTALL_FAILED",
                "REDIRECTED_TO_PLAY", "NEW_RELEASE_NOT_AVAILABLE", "NEW_RELEASE_CHECK_FAILED", "UPDATE_CANCELED",
            ),
            UpdateStatus.entries.map { it.name },
        )
        assertEquals(
            listOf(
                "UNKNOWN", "AUTHENTICATION_FAILURE", "AUTHENTICATION_CANCELED", "NETWORK_FAILURE", "DOWNLOAD_FAILURE",
                "INSTALLATION_FAILURE", "INSTALLATION_CANCELED", "UPDATE_NOT_AVAILABLE", "HOST_ACTIVITY_INTERRUPTED",
                "NOT_IMPLEMENTED", "API_DISABLED",
            ),
            Status.entries.map { it.name },
        )
        assertEquals(UpdateStatus.REDIRECTED_TO_PLAY, UpdateStatus.valueOf("REDIRECTED_TO_PLAY"))
    }

    /**
     * Not run: members that present UI (sign-in, the update dialog) or need a release, and the destructuring
     * declarations, which need SDK instances (on Android the release and progress types are Java interfaces, which
     * common code cannot implement).
     */
    @Suppress("unused")
    private fun compileOnly(appDistribution: FirebaseAppDistribution, release: AppDistributionRelease, progress: UpdateProgress): List<Any?> {
        appDistribution.signInTester()
            .addOnSuccessListener { }
            .addOnFailureListener { e -> (e as? FirebaseAppDistributionException)?.errorCode }
        appDistribution.checkForNewRelease().addOnSuccessListener { newRelease -> newRelease?.displayVersion }
        appDistribution.updateIfNewReleaseAvailable()
            .addOnProgressListener { updateProgress -> updateProgress.apkBytesDownloaded }
            .addOnCanceledListener { }
            .addOnCompleteListener { task -> task.isSuccessful }
        appDistribution.startFeedback(0)
        appDistribution.startFeedback(0, null)
        appDistribution.showFeedbackNotification(0, InterruptionLevel.DEFAULT)
        val (binaryType, displayVersion, versionCode, releaseNotes) = release
        val (apkBytesDownloaded, apkFileTotalBytes, updateStatus) = progress
        return listOf(binaryType, displayVersion, versionCode, releaseNotes, apkBytesDownloaded, apkFileTotalBytes, updateStatus)
    }
}
