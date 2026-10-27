/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.remoteconfig

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import io.github.corneloaie.firebase.apps
import io.github.corneloaie.firebase.initialize
import io.github.corneloaie.firebase.runTest
import kotlinx.coroutines.tasks.await
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The real-time update part of the `com.google.firebase.remoteconfig` layer; custom signals are in CustomSignalsTest. */
@IgnoreForAndroidUnitTest
@IgnoreForJvm
class ConfigUpdateTest {

    @BeforeTest
    fun initializeFirebase() {
        io.github.corneloaie.firebase.Firebase.apps(context).firstOrNull() ?: io.github.corneloaie.firebase.Firebase.initialize(
            context,
            io.github.corneloaie.firebase.FirebaseOptions(
                applicationId = "1:846484016111:ios:dd1f6688bad7af768c841a",
                apiKey = "AIzaSyCK87dcMFhzCz_kJVs2cT2AVlqOTLuyWV0",
                databaseUrl = "https://fir-kotlin-sdk.firebaseio.com",
                storageBucket = "fir-kotlin-sdk.appspot.com",
                projectId = "fir-kotlin-sdk",
                gcmSenderId = "846484016111",
            ),
        )
    }

    // The app is left initialised: deleting it asynchronously (as the JS SDK does) would race the next test's setup.
    @AfterTest
    fun tearDown() = runTest {
        FirebaseRemoteConfig.getInstance().reset().await()
    }

    @Test
    fun testConfigUpdateListener() {
        val registration = Firebase.remoteConfig.addOnConfigUpdateListener(
            object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) = Unit

                override fun onError(error: FirebaseRemoteConfigException) = Unit
            },
        )
        registration.remove()
        assertEquals(setOf("a", "b"), ConfigUpdate.create(setOf("a", "b")).updatedKeys)
    }
}
