/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck.debug

import swiftPMImport.dev.gitlive.firebase.appcheck.FIRAppCheckDebugProviderFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.createProvider

/** @property ios The underlying Firebase iOS SDK factory, which is installed as it is. */
public actual class DebugAppCheckProviderFactory private constructor(public val ios: FIRAppCheckDebugProviderFactory) : AppCheckProviderFactory {
    actual override fun create(firebaseApp: FirebaseApp): AppCheckProvider = ios.createProvider(firebaseApp)

    override fun toString(): String = "DebugAppCheckProviderFactory"

    public actual companion object {
        private val instance = DebugAppCheckProviderFactory(FIRAppCheckDebugProviderFactory())

        public actual fun getInstance(): DebugAppCheckProviderFactory = instance
    }
}
