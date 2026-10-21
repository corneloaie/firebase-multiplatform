/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.appcheck

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.AppCheckToken
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.apps
import dev.gitlive.firebase.initialize

expect val context: Any
expect annotation class IgnoreForAndroidUnitTest()

/**
 * The iOS SDK reads the provider factory when an app is configured, before a test can install one, so the tests that
 * need the fake provider's tokens do not run on Apple platforms.
 */
expect annotation class IgnoreForApple()

/** Initializes the default app once per test process, which the tests share. */
fun initializeTestApp() {
    Firebase.apps(context).ifEmpty {
        Firebase.initialize(
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

/** A JWT whose `exp` claim is [FAKE_EXPIRE_TIME_MILLIS], so that the JS SDK, which reports the JWT alone, agrees. */
const val FAKE_TOKEN = "eyJhbGciOiJub25lIn0.eyJleHAiOjIxNDU5MTY4MDB9.c2ln"

/**
 * 1 January 2038. The Android SDK reads a token's `exp` claim as a 32-bit `Integer`, so a later one (past 19 January
 * 2038) fails with a ClassCastException there.
 */
const val FAKE_EXPIRE_TIME_MILLIS = 2_145_916_800_000L

const val FAKE_LIMITED_USE_TOKEN = "limited-use"

/**
 * A token subclass as Android code writes one: the Android SDK's class is Java, so it overrides the getters. Its own
 * properties are not named `token` or `expireTimeMillis`, which [AppCheckToken] declares to read the getters.
 */
class FakeToken(private val value: String, private val expiration: Long) : AppCheckToken() {
    override fun getToken(): String = value

    override fun getExpireTimeMillis(): Long = expiration
}

/**
 * A custom provider, as an app attesting itself with its own backend would write: App Check uses its tokens as they
 * are, without a request to the App Check backend.
 */
class FakeProvider : AppCheckProvider {
    override fun getToken(): Task<AppCheckToken> = completed(FakeToken(FAKE_TOKEN, FAKE_EXPIRE_TIME_MILLIS))

    override fun getLimitedUseToken(): Task<AppCheckToken> = completed(FakeToken(FAKE_LIMITED_USE_TOKEN, FAKE_EXPIRE_TIME_MILLIS))
}

class FakeProviderFactory : AppCheckProviderFactory {
    override fun create(firebaseApp: FirebaseApp): AppCheckProvider = FakeProvider()
}

/** A provider whose token requests fail with [message]. */
class FailingProviderFactory(private val message: String) : AppCheckProviderFactory {
    override fun create(firebaseApp: FirebaseApp): AppCheckProvider = AppCheckProvider {
        TaskCompletionSource<AppCheckToken>().apply { setException(IllegalStateException(message)) }.task
    }
}

private fun completed(token: AppCheckToken): Task<AppCheckToken> = TaskCompletionSource<AppCheckToken>().apply { setResult(token) }.task
