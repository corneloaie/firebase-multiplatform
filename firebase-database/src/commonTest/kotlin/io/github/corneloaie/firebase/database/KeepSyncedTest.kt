/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.database

import com.google.firebase.database.FirebaseDatabase
import io.github.corneloaie.firebase.UnsupportedOnJs
import io.github.corneloaie.firebase.apps
import io.github.corneloaie.firebase.initialize
import io.github.corneloaie.firebase.runBlockingTest
import io.github.corneloaie.firebase.runTest
import kotlinx.coroutines.tasks.await
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** The offline synchronization part of the `com.google.firebase.database` layer; a no-op on JS, whose SDK has no persistent cache. */
@OptIn(UnsupportedOnJs::class)
@IgnoreForAndroidUnitTest
class KeepSyncedTest {

    private lateinit var database: FirebaseDatabase

    @BeforeTest
    fun initializeFirebase() {
        val app = io.github.corneloaie.firebase.Firebase.apps(context).firstOrNull() ?: io.github.corneloaie.firebase.Firebase.initialize(
            context,
            io.github.corneloaie.firebase.FirebaseOptions(
                applicationId = "1:846484016111:ios:dd1f6688bad7af768c841a",
                apiKey = "AIzaSyCK87dcMFhzCz_kJVs2cT2AVlqOTLuyWV0",
                databaseUrl = "https://fir-kotlin-sdk-default-rtdb.firebaseio.com",
                storageBucket = "fir-kotlin-sdk.appspot.com",
                projectId = "fir-kotlin-sdk-default-rtdb",
                gcmSenderId = "846484016111",
            ),
        )
        database = FirebaseDatabase.getInstance(app.compat).apply { useEmulator(emulatorHost, 9000) }
    }

    @AfterTest
    fun deinitializeFirebase() = runBlockingTest {
        io.github.corneloaie.firebase.Firebase.apps(context).forEach { it.delete() }
    }

    @Test
    fun testKeepSynced() = runTest {
        val reference = database.getReference("test/compatKeepSynced")
        reference.setValue("synced").await()
        reference.keepSynced(true)
        assertEquals("synced", reference.get().await().value)
        reference.keepSynced(false)
        reference.orderByKey().limitToFirst(1).keepSynced(false)
    }
}
