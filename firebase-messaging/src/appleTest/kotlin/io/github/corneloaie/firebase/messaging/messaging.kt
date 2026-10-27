/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.messaging

import io.github.corneloaie.firebase.Firebase
import io.github.corneloaie.firebase.FirebaseOptions
import io.github.corneloaie.firebase.apps
import io.github.corneloaie.firebase.initialize
import kotlin.test.BeforeTest

// FIRMessaging.messaging() is nil until the default app is configured, so the tests configure it first.
private fun initializeFirebase() {
    Firebase.apps(Unit).firstOrNull() ?: Firebase.initialize(
        Unit,
        FirebaseOptions(
            applicationId = "1:846484016111:ios:dd1f6688bad7af768c841a",
            apiKey = "AIzaSyCK87dcMFhzCz_kJVs2cT2AVlqOTLuyWV0",
            databaseUrl = "https://fir-kotlin-sdk.firebaseio.com",
            storageBucket = "fir-kotlin-sdk.appspot.com",
            projectId = "fir-kotlin-sdk",
            gcmSenderId = "846484016111",
        ),
    )
}

class IOSFirebaseMessagingTest : FirebaseMessagingTest() {
    @BeforeTest
    fun initialize() = initializeFirebase()
}
