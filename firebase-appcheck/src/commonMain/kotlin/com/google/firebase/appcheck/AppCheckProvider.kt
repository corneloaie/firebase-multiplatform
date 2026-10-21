/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck

import com.google.android.gms.tasks.Task

/*
 * Plain common declarations, verified against firebase-appcheck and stripped on Android (see AppCheckToken.kt).
 */

/**
 * Produces [AppCheckToken]s, mirroring `com.google.firebase.appcheck.AppCheckProvider`. App Check calls it whenever
 * it needs a new token. Implement it, with an [AppCheckProviderFactory], to attest the app with a backend of your
 * own: its tokens are used as they are.
 */
public fun interface AppCheckProvider {
    /** Returns a new, valid token, or fails when the app cannot be attested. */
    public fun getToken(): Task<AppCheckToken>

    /** Returns a new limited-use token, for a backend with replay protection; [getToken] unless overridden. */
    public fun getLimitedUseToken(): Task<AppCheckToken> = getToken()
}
