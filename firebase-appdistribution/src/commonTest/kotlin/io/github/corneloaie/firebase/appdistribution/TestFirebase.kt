/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.appdistribution

import io.github.corneloaie.firebase.Firebase
import io.github.corneloaie.firebase.FirebaseOptions
import io.github.corneloaie.firebase.apps
import io.github.corneloaie.firebase.initialize

expect val context: Any

/** Android unit tests have no Android runtime to initialize Firebase in, so the tests that need it are skipped there. */
expect annotation class IgnoreForAndroidUnitTest()

/** Initializes the default Firebase app once per test process, which App Distribution belongs to. */
fun initializeFirebase() {
    if (Firebase.apps(context).isEmpty()) {
        Firebase.initialize(
            context,
            FirebaseOptions(
                applicationId = "1:846484016111:ios:dd1f6688bad7af768c841a",
                apiKey = "AIzaSyB7pZ7tXymW9WC_ozAppbEs9WBffSmfX9c",
                databaseUrl = "https://fir-kotlin-sdk.firebaseio.com",
                storageBucket = "fir-kotlin-sdk.appspot.com",
                projectId = "fir-kotlin-sdk",
                gcmSenderId = "846484016111",
            ),
        )
    }
}
