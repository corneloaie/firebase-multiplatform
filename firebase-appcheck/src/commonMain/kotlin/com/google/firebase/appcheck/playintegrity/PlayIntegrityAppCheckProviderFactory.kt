/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck.playintegrity

import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory

/**
 * The factory of the Play Integrity provider, mirroring
 * `com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory` (firebase-appcheck-playintegrity),
 * which attests an Android app with the Play Integrity API.
 *
 * Play Integrity exists on Android only: on Apple and JS the provider fails every token request, and the members
 * carry `@UnsupportedOnApple` and `@UnsupportedOnJs`. Use DeviceCheck or App Attest on Apple, and reCAPTCHA on the web.
 */
public expect class PlayIntegrityAppCheckProviderFactory() : AppCheckProviderFactory {
    /** Returns the Play Integrity provider of [firebaseApp]. */
    override fun create(firebaseApp: FirebaseApp): AppCheckProvider

    public companion object {
        /** Returns the shared [PlayIntegrityAppCheckProviderFactory]. */
        public fun getInstance(): PlayIntegrityAppCheckProviderFactory
    }
}
