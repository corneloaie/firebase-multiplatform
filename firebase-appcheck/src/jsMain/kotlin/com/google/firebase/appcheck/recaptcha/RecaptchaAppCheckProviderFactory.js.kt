/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck.recaptcha

import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.unavailableProvider

/**
 * Installs the JS SDK's `ReCaptchaEnterpriseProvider`. The JS SDK runs that provider itself, so [create]'s provider
 * fails: its tokens come through [com.google.firebase.appcheck.FirebaseAppCheck.getAppCheckToken].
 */
public actual class RecaptchaAppCheckProviderFactory private constructor(internal val siteKey: String) : AppCheckProviderFactory {
    actual override fun create(firebaseApp: FirebaseApp): AppCheckProvider = unavailableProvider(
        "The Firebase JS SDK runs its reCAPTCHA Enterprise provider itself: install this factory and use FirebaseAppCheck.getAppCheckToken",
    )

    override fun toString(): String = "RecaptchaAppCheckProviderFactory"

    public actual companion object {
        public actual fun getInstance(siteKey: String): RecaptchaAppCheckProviderFactory = RecaptchaAppCheckProviderFactory(siteKey)
    }
}
