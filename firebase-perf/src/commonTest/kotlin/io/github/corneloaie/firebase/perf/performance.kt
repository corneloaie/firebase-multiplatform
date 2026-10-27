/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.perf

import io.github.corneloaie.firebase.Firebase
import io.github.corneloaie.firebase.FirebaseOptions
import io.github.corneloaie.firebase.apps
import io.github.corneloaie.firebase.initialize
import io.github.corneloaie.firebase.runTest
import kotlin.test.*

expect val context: Any
expect annotation class IgnoreForAndroidUnitTest()

@IgnoreForAndroidUnitTest
class FirebasePerformanceTest {

    lateinit var performance: FirebasePerformance

    @BeforeTest
    fun initializeFirebase() {
        val app = Firebase.apps(context).firstOrNull() ?: Firebase.initialize(
            context,
            FirebaseOptions(
                applicationId = "1:846484016111:ios:dd1f6688bad7af768c841a",
                apiKey = "AIzaSyCK87dcMFhzCz_kJVs2cT2AVlqOTLuyWV0",
                databaseUrl = "https://fir-kotlin-sdk.firebaseio.com",
                storageBucket = "fir-kotlin-sdk.appspot.com",
                projectId = "fir-kotlin-sdk",
                gcmSenderId = "846484016111",
            ),
        )

        performance = Firebase.performance(app)
    }

    // The default app is kept for the whole test process instead of being deleted after each test: with BoM 35,
    // Firebase Sessions (which Performance pulls in) opens a DataStore per app initialisation and crashes the process
    // ("There are multiple DataStores active for the same file") when the default app is re-created, and the background
    // installation work fails on a deleted app.

    @Test
    fun testNewTrace() = runTest {
        val trace = performance.newTrace("Test Trace")

        assertNotNull(trace)
    }

    @Test
    fun testPerformanceCollectionEnabled() = runTest {
        performance.setPerformanceCollectionEnabled(false)

        assertFalse(performance.isPerformanceCollectionEnabled())

        performance.setPerformanceCollectionEnabled(true)

        assertTrue(performance.isPerformanceCollectionEnabled())
    }
}
