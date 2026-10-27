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

actual val context: Any = Unit

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class IgnoreForAndroidUnitTest

class JsPerformanceTest {

    private lateinit var performance: FirebasePerformance

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

    @Test
    fun testInstrumentationEnabled() = runTest {
        performance.setInstrumentationEnabled(false)

        assertFalse(performance.isInstrumentationEnabled())

        performance.setInstrumentationEnabled(true)

        assertTrue(performance.isInstrumentationEnabled())
    }
}
