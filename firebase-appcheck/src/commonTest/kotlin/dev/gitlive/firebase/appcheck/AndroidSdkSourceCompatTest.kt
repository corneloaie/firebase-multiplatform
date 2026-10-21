/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.appcheck

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.AppCheckToken
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.component1
import com.google.firebase.appcheck.component2
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import dev.gitlive.firebase.UnsupportedOnApple
import dev.gitlive.firebase.UnsupportedOnJs
import dev.gitlive.firebase.runTest
import kotlinx.coroutines.tasks.await
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Exercises the `com.google.firebase.appcheck` layer exactly as Android app code would (static accessors, the
 * `Firebase.appCheck` extensions, a custom provider with its own token class, the Task API and listeners), on every
 * platform.
 */
@IgnoreForAndroidUnitTest
class AndroidSdkSourceCompatTest {

    @BeforeTest
    fun initializeFirebase() {
        initializeTestApp()
    }

    @Test
    fun testInstances() {
        val appCheck: FirebaseAppCheck = FirebaseAppCheck.getInstance()
        assertSame(appCheck, FirebaseAppCheck.getInstance(FirebaseApp.getInstance()))
        assertSame(appCheck, Firebase.appCheck)
        assertSame(appCheck, Firebase.appCheck(FirebaseApp.getInstance()))
        assertSame(appCheck, dev.gitlive.firebase.Firebase.appCheck.compat)
    }

    @Test
    @OptIn(UnsupportedOnApple::class, UnsupportedOnJs::class)
    fun testProviderFactories() {
        val debug: AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
        assertSame(debug, DebugAppCheckProviderFactory.getInstance())
        val playIntegrity: AppCheckProviderFactory = PlayIntegrityAppCheckProviderFactory.getInstance()
        assertSame(playIntegrity, PlayIntegrityAppCheckProviderFactory.getInstance())
        val constructed: AppCheckProviderFactory = PlayIntegrityAppCheckProviderFactory()
        assertTrue(constructed !== playIntegrity)
        val recaptcha: AppCheckProviderFactory = RecaptchaAppCheckProviderFactory.getInstance("6Lc-site-key")
        assertTrue(recaptcha !== debug)
    }

    @Test
    @IgnoreForApple
    fun testCustomProviderToken() = runTest {
        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.installAppCheckProviderFactory(FakeProviderFactory(), false)
        val task: Task<AppCheckToken> = appCheck.getAppCheckToken(true)
        val token = task.await()
        assertTrue(task.isSuccessful)
        assertEquals(FAKE_TOKEN, token.token)
        assertEquals(FAKE_EXPIRE_TIME_MILLIS, token.expireTimeMillis)
        val (raw, expireTimeMillis) = token
        assertEquals(FAKE_TOKEN, raw)
        assertEquals(FAKE_EXPIRE_TIME_MILLIS, expireTimeMillis)
        // The token is valid until 2038, so it is cached.
        assertEquals(FAKE_TOKEN, appCheck.getAppCheckToken(false).await().getToken())
        assertEquals(FAKE_LIMITED_USE_TOKEN, appCheck.getLimitedUseAppCheckToken().await().token)
        appCheck.setTokenAutoRefreshEnabled(false)
    }

    @Test
    @IgnoreForApple
    fun testAppCheckListener() = runTest {
        val appCheck = Firebase.appCheck
        appCheck.installAppCheckProviderFactory(FakeProviderFactory())
        // The JS SDK would refresh the fake token, which expires in 2038, in a loop: its timer overflows.
        appCheck.setTokenAutoRefreshEnabled(false)
        val received = TaskCompletionSource<AppCheckToken>()
        val listener = FirebaseAppCheck.AppCheckListener { received.trySetResult(it) }
        appCheck.addAppCheckListener(listener)
        try {
            // Android calls listeners on the main thread, so the token is handed over rather than asserted there.
            appCheck.getAppCheckToken(true).await()
            assertEquals(FAKE_TOKEN, received.task.await().token)
        } finally {
            appCheck.removeAppCheckListener(listener)
        }
    }

    @Test
    @IgnoreForApple
    fun testProviderFailure() = runTest {
        val appCheck = FirebaseAppCheck.getInstance()
        appCheck.installAppCheckProviderFactory(FailingProviderFactory("attestation failed"), false)
        try {
            val error = assertFails { appCheck.getAppCheckToken(true).await() }
            assertTrue(error.message.orEmpty().contains("attestation failed"), "$error")
        } finally {
            appCheck.installAppCheckProviderFactory(FakeProviderFactory(), false)
        }
    }
}
