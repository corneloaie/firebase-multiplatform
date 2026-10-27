/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.appdistribution

import io.github.corneloaie.firebase.Firebase
import io.github.corneloaie.firebase.runTest
import kotlinx.coroutines.flow.toList
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Kotlin-first API without a signed-in tester (and, on Android, with the API-only library): the operations fail
 * with NOT_IMPLEMENTED on Android and the JVM, and with AUTHENTICATION_FAILURE on iOS.
 */
@IgnoreForAndroidUnitTest
class FirebaseAppDistributionTest {

    @BeforeTest
    fun initialize() = initializeFirebase()

    private val expectedErrors = setOf(AppDistributionExceptionStatus.NOT_IMPLEMENTED, AppDistributionExceptionStatus.AUTHENTICATION_FAILURE)

    @Test
    fun testInstance() {
        assertEquals(Firebase.appDistribution, Firebase.appDistribution)
        assertFalse(Firebase.appDistribution.isTesterSignedIn)
        Firebase.appDistribution.signOutTester()
    }

    @Test
    fun testCheckForNewRelease() = runTest {
        val e = assertFailsWith<FirebaseAppDistributionException> { Firebase.appDistribution.checkForNewRelease() }
        assertTrue(e.errorCode in expectedErrors, "${e.errorCode}")
    }

    @Test
    fun testUpdateApp() = runTest {
        val progress = mutableListOf<UpdateProgress>()
        val e = assertFailsWith<FirebaseAppDistributionException> { Firebase.appDistribution.updateApp().toList(progress) }
        assertTrue(e.errorCode in expectedErrors, "${e.errorCode}")
        assertTrue(progress.isEmpty())
    }

    @Test
    fun testFeedback() {
        Firebase.appDistribution.startFeedback("Feedback")
        Firebase.appDistribution.showFeedbackNotification("Feedback")
        Firebase.appDistribution.cancelFeedbackNotification()
    }
}
