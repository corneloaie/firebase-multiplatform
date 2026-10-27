/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.appcheck

import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.JsFirebaseAppCheck
import com.google.firebase.appcheck.unavailableProvider
import io.github.corneloaie.firebase.appcheck.externals.AppCheck

/** The underlying Firebase JS SDK object; null until a provider factory is installed. */
public val FirebaseAppCheck.js: AppCheck? get() = (compat as? JsFirebaseAppCheck)?.js

/**
 * JS only: the factory of the JS SDK's `ReCaptchaV3Provider`, for a reCAPTCHA v3 [siteKey]. The JS SDK runs that
 * provider itself, so [create]'s provider fails: its tokens come through [FirebaseAppCheck.getAppCheckToken].
 */
public class ReCaptchaV3ProviderFactory(public val siteKey: String) : AppCheckProviderFactory {
    override fun create(firebaseApp: FirebaseApp): AppCheckProvider = unavailableProvider(
        "The Firebase JS SDK runs its reCAPTCHA v3 provider itself: install this factory and use FirebaseAppCheck.getAppCheckToken",
    )

    override fun toString(): String = "ReCaptchaV3ProviderFactory"
}
