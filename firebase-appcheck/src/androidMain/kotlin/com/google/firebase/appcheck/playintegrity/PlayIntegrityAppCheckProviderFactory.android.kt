/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck.playintegrity

import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import io.github.corneloaie.firebase.appcheck.stub

/* Header stub for com.google.firebase:firebase-appcheck-playintegrity (see buildSrc utils/HeaderStubs.kt). */

public actual class PlayIntegrityAppCheckProviderFactory actual constructor() : AppCheckProviderFactory {
    actual override fun create(firebaseApp: FirebaseApp): AppCheckProvider = stub()

    public actual companion object {
        @JvmStatic
        public actual fun getInstance(): PlayIntegrityAppCheckProviderFactory = stub()
    }
}
