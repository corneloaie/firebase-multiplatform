/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck.playintegrity

import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.unavailableProvider
import io.github.corneloaie.firebase.UnsupportedOnApple

/** Play Integrity is an Android API: the provider fails every token request on Apple platforms. */
public actual class PlayIntegrityAppCheckProviderFactory @UnsupportedOnApple actual constructor() : AppCheckProviderFactory {
    actual override fun create(firebaseApp: FirebaseApp): AppCheckProvider = unavailableProvider(
        "Play Integrity is not available on Apple platforms; use DeviceCheckProviderFactory or AppAttestProviderFactory",
    )

    override fun toString(): String = "PlayIntegrityAppCheckProviderFactory"

    public actual companion object {
        @OptIn(UnsupportedOnApple::class)
        private val instance = PlayIntegrityAppCheckProviderFactory()

        /** The factory of a provider that fails every token request: Play Integrity is an Android API. */
        @UnsupportedOnApple
        public actual fun getInstance(): PlayIntegrityAppCheckProviderFactory = instance
    }
}
