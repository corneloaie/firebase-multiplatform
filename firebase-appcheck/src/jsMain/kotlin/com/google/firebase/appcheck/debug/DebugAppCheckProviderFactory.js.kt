/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck.debug

import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.unavailableProvider

/**
 * Installing it turns on the JS SDK's debug mode, in which the JS SDK exchanges the debug token itself, so App Check
 * only falls back on [create]'s provider when debug mode came too late: it fails with an explanation.
 */
public actual class DebugAppCheckProviderFactory private constructor() : AppCheckProviderFactory {
    actual override fun create(firebaseApp: FirebaseApp): AppCheckProvider = unavailableProvider(
        "The Firebase JS SDK is not in debug mode: it reads FIREBASE_APPCHECK_DEBUG_TOKEN when App Check first starts in the page, " +
            "so install DebugAppCheckProviderFactory before any other factory",
    )

    override fun toString(): String = "DebugAppCheckProviderFactory"

    public actual companion object {
        private val instance = DebugAppCheckProviderFactory()

        public actual fun getInstance(): DebugAppCheckProviderFactory = instance
    }
}
