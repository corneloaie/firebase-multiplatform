/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck.recaptcha

import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.unavailableProvider
import io.github.corneloaie.firebase.UnsupportedOnApple

/** The iOS SDK has no reCAPTCHA provider on tvOS: the provider fails every token request. */
public actual class RecaptchaAppCheckProviderFactory private constructor() : AppCheckProviderFactory {
    actual override fun create(firebaseApp: FirebaseApp): AppCheckProvider = unavailableProvider(
        "reCAPTCHA is not available on tvOS; use DeviceCheckProviderFactory or AppAttestProviderFactory",
    )

    override fun toString(): String = "RecaptchaAppCheckProviderFactory"

    public actual companion object {
        private val instance = RecaptchaAppCheckProviderFactory()

        /** The factory of a provider that fails every token request: the iOS SDK has no reCAPTCHA provider on tvOS. */
        @UnsupportedOnApple
        public actual fun getInstance(siteKey: String): RecaptchaAppCheckProviderFactory = instance
    }
}
