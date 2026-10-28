package io.github.corneloaie.firebase.messaging

import androidx.test.platform.app.InstrumentationRegistry
import io.github.corneloaie.firebase.Firebase
import io.github.corneloaie.firebase.FirebaseOptions
import io.github.corneloaie.firebase.apps
import io.github.corneloaie.firebase.initialize
import kotlin.test.BeforeTest

class AndroidInstrumentedFirebaseMessagingTest : FirebaseMessagingTest() {

    private val context = InstrumentationRegistry.getInstrumentation().context

    @BeforeTest
    fun initializeFirebase() {
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
    }
}
