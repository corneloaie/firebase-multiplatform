/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck.playintegrity

import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.unavailableProvider
import io.github.corneloaie.firebase.UnsupportedOnJs

/** Play Integrity is an Android API: the provider fails every token request on JS. */
public actual class PlayIntegrityAppCheckProviderFactory @UnsupportedOnJs actual constructor() : AppCheckProviderFactory {
    actual override fun create(firebaseApp: FirebaseApp): AppCheckProvider = unavailableProvider(
        "Play Integrity is not available on the web; use RecaptchaAppCheckProviderFactory or ReCaptchaV3ProviderFactory",
    )

    override fun toString(): String = "PlayIntegrityAppCheckProviderFactory"

    public actual companion object {
        @OptIn(UnsupportedOnJs::class)
        private val instance = PlayIntegrityAppCheckProviderFactory()

        /** The factory of a provider that fails every token request: Play Integrity is an Android API. */
        @UnsupportedOnJs
        public actual fun getInstance(): PlayIntegrityAppCheckProviderFactory = instance
    }
}
