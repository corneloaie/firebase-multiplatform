/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck.recaptcha

import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory

/**
 * The factory of the reCAPTCHA Enterprise provider, mirroring
 * `com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory` (firebase-appcheck-recaptcha).
 *
 * Each platform needs a reCAPTCHA Enterprise key of its own kind:
 * - **Android:** an Android key.
 * - **iOS:** an iOS key. The iOS SDK's `RecaptchaProviderFactory` is a public preview, and needs the reCAPTCHA
 *   Enterprise iOS SDK linked into the app.
 * - **JS:** a score-based web key, used with the JS SDK's `ReCaptchaEnterpriseProvider`.
 *
 * macOS and tvOS have no reCAPTCHA provider: there the provider fails every token request, and the members carry
 * `@UnsupportedOnApple`.
 */
public expect class RecaptchaAppCheckProviderFactory : AppCheckProviderFactory {
    /** Returns the reCAPTCHA provider of [firebaseApp]. */
    override fun create(firebaseApp: FirebaseApp): AppCheckProvider

    public companion object {
        /** Returns a factory of reCAPTCHA providers for the reCAPTCHA Enterprise [siteKey]. */
        public fun getInstance(siteKey: String): RecaptchaAppCheckProviderFactory
    }
}
