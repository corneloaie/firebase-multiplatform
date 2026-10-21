/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.inappmessaging

import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplay
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks.InAppMessagingErrorReason
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.apps
import dev.gitlive.firebase.initialize
import dev.gitlive.firebase.runTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import com.google.firebase.inappmessaging.display.FirebaseInAppMessagingDisplay as DefaultDisplay

expect val context: Any

/** Android unit tests have no Android runtime, which the In-App Messaging SDK needs, so these tests are skipped there. */
expect annotation class IgnoreForAndroidUnitTest()

/** Creates the SDK instance where an app would, if the platform cares on which thread that happens. */
internal expect fun createInAppMessaging()

/** Configures the default app once per test process: In-App Messaging only works with the default app. */
internal fun initializeFirebase() {
    Firebase.apps(context).firstOrNull() ?: Firebase.initialize(
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
    createInAppMessaging()
}

@IgnoreForAndroidUnitTest
class FirebaseInAppMessagingTest {

    @BeforeTest
    fun initialize() {
        initializeFirebase()
    }

    @Test
    fun testSettings() {
        val inAppMessaging = Firebase.inAppMessaging
        assertEquals(inAppMessaging, Firebase.inAppMessaging)

        inAppMessaging.messagesSuppressed = true
        assertTrue(inAppMessaging.messagesSuppressed)
        inAppMessaging.messagesSuppressed = false
        assertFalse(inAppMessaging.messagesSuppressed)

        val enabled = inAppMessaging.isAutomaticDataCollectionEnabled
        inAppMessaging.isAutomaticDataCollectionEnabled = !enabled
        assertEquals(!enabled, inAppMessaging.isAutomaticDataCollectionEnabled)
        inAppMessaging.setAutomaticDataCollectionEnabled(null)
        inAppMessaging.isAutomaticDataCollectionEnabled = enabled

        inAppMessaging.triggerEvent("dev_gitlive_test_event")
    }

    @Test
    fun testFlows() = runTest {
        val inAppMessaging = Firebase.inAppMessaging
        // No campaign targets the test app: each collection registers a listener, and the timeout removes it again.
        assertNull(withTimeoutOrNull(100.milliseconds) { inAppMessaging.clicks.first() })
        assertNull(withTimeoutOrNull(100.milliseconds) { inAppMessaging.impressions.first() })
        assertNull(withTimeoutOrNull(100.milliseconds) { inAppMessaging.dismissals.first() })
        assertNull(withTimeoutOrNull(100.milliseconds) { inAppMessaging.displayErrors.first() })
    }

    @Test
    fun testDisplayComponent() {
        val inAppMessaging = Firebase.inAppMessaging
        inAppMessaging.setMessageDisplayComponent(
            FirebaseInAppMessagingDisplay { _, callbacks -> callbacks.displayErrorEncountered(InAppMessagingErrorReason.UNSPECIFIED_RENDER_ERROR) },
        )
        inAppMessaging.setMessageDisplayComponent(null)
        inAppMessaging.setMessageDisplayComponent(DefaultDisplay.getInstance())
    }
}
