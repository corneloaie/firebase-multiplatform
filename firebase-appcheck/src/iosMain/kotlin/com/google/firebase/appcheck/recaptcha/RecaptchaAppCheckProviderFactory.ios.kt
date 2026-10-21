/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck.recaptcha

import swiftPMImport.dev.gitlive.firebase.appcheck.FIRAppCheckProviderFactoryProtocol
import swiftPMImport.dev.gitlive.firebase.appcheck.FIRRecaptchaProviderFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.createProvider

/**
 * The iOS SDK's `RecaptchaProviderFactory`, a public preview. The app must link the reCAPTCHA Enterprise iOS SDK, which
 * the provider looks up at runtime.
 *
 * @property ios The underlying Firebase iOS SDK factory, which is installed as it is.
 */
public actual class RecaptchaAppCheckProviderFactory private constructor(public val ios: FIRRecaptchaProviderFactory) : AppCheckProviderFactory {
    actual override fun create(firebaseApp: FirebaseApp): AppCheckProvider = ios.createProvider(firebaseApp)

    override fun toString(): String = "RecaptchaAppCheckProviderFactory"

    public actual companion object {
        public actual fun getInstance(siteKey: String): RecaptchaAppCheckProviderFactory = RecaptchaAppCheckProviderFactory(FIRRecaptchaProviderFactory(siteKey))
    }
}
