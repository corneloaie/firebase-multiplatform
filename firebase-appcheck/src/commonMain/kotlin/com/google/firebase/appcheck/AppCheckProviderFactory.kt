/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck

import com.google.firebase.FirebaseApp

/*
 * A plain common declaration, verified against firebase-appcheck and stripped on Android (see AppCheckToken.kt).
 */

/**
 * Creates the [AppCheckProvider] of a [FirebaseApp], mirroring `com.google.firebase.appcheck.AppCheckProviderFactory`.
 * Install one with [FirebaseAppCheck.installAppCheckProviderFactory].
 */
public fun interface AppCheckProviderFactory {
    /** Returns the provider of [firebaseApp], creating it if needed. */
    public fun create(firebaseApp: FirebaseApp): AppCheckProvider
}
