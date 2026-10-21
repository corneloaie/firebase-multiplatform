/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.appcheck

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.app
import dev.gitlive.firebase.runTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@IgnoreForAndroidUnitTest
class FirebaseAppCheckTest {

    @BeforeTest
    fun initializeFirebase() {
        initializeTestApp()
    }

    @Test
    fun testInstances() {
        assertEquals(Firebase.appCheck, Firebase.appCheck(Firebase.app))
    }

    @Test
    @IgnoreForApple
    fun testGetAppCheckToken() = runTest {
        val appCheck = Firebase.appCheck
        appCheck.installAppCheckProviderFactory(FakeProviderFactory(), isTokenAutoRefreshEnabled = false)
        val (token, expireTimeMillis) = appCheck.getAppCheckToken(forceRefresh = true)
        assertEquals(FAKE_TOKEN, token)
        assertEquals(FAKE_EXPIRE_TIME_MILLIS, expireTimeMillis)
        assertEquals(FAKE_LIMITED_USE_TOKEN, appCheck.getLimitedUseAppCheckToken().token)
    }

    @Test
    @IgnoreForApple
    fun testAppCheckTokenChanged() = runTest {
        val appCheck = Firebase.appCheck
        appCheck.installAppCheckProviderFactory(FakeProviderFactory(), isTokenAutoRefreshEnabled = false)
        val received = CompletableDeferred<AppCheckToken>()
        // Unconfined and undispatched, the flow registers its listener before the token request below.
        val collector = launch(Dispatchers.Unconfined, start = CoroutineStart.UNDISPATCHED) {
            appCheck.appCheckTokenChanged.collect { received.complete(it) }
        }
        try {
            appCheck.getAppCheckToken(forceRefresh = true)
            assertEquals(FAKE_TOKEN, received.await().token)
        } finally {
            collector.cancel()
        }
    }
}
