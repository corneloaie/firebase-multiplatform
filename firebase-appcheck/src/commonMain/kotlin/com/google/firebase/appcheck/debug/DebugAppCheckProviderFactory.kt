/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck.debug

import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory

/**
 * The factory of the debug provider, mirroring `com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory`
 * (firebase-appcheck-debug). For emulators, simulators and CI: the provider exchanges a debug token for App Check
 * tokens, once you have added that debug token in the Firebase console. Do not use it in builds for real users.
 *
 * The debug token comes from the platform SDK:
 * - **Android** generates one, keeps it and logs it.
 * - **Apple** reads the `AppCheckDebugToken` environment variable, or generates one, keeps it and logs it.
 * - **JS** switches the JS SDK to its debug mode (`self.FIREBASE_APPCHECK_DEBUG_TOKEN = true`, unless the page set a
 *   token there itself), which logs a generated token to the console. The JS SDK reads that setting the first time
 *   App Check starts in the page, so install this factory before any other.
 */
public expect class DebugAppCheckProviderFactory : AppCheckProviderFactory {
    /** Returns the debug provider of [firebaseApp]. */
    override fun create(firebaseApp: FirebaseApp): AppCheckProvider

    public companion object {
        /** Returns the [DebugAppCheckProviderFactory] singleton. */
        public fun getInstance(): DebugAppCheckProviderFactory
    }
}
