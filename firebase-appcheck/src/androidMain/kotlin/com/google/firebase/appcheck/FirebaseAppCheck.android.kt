/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import io.github.corneloaie.firebase.appcheck.stub

/*
 * Header stubs for com.google.firebase:firebase-appcheck (see buildSrc utils/HeaderStubs.kt): compiled against,
 * verified to match the real classes, and deleted from the output so the real SDK binds at runtime.
 */

public actual abstract class FirebaseAppCheck actual constructor() {
    public actual abstract fun installAppCheckProviderFactory(factory: AppCheckProviderFactory)
    public actual abstract fun installAppCheckProviderFactory(factory: AppCheckProviderFactory, isTokenAutoRefreshEnabled: Boolean)
    public actual abstract fun setTokenAutoRefreshEnabled(isTokenAutoRefreshEnabled: Boolean)
    public actual abstract fun getAppCheckToken(forceRefresh: Boolean): Task<AppCheckToken>
    public actual abstract fun getLimitedUseAppCheckToken(): Task<AppCheckToken>
    public actual abstract fun addAppCheckListener(listener: AppCheckListener)
    public actual abstract fun removeAppCheckListener(listener: AppCheckListener)

    public actual fun interface AppCheckListener {
        public actual fun onAppCheckTokenChanged(token: AppCheckToken)
    }

    public actual companion object {
        @JvmStatic
        public actual fun getInstance(): FirebaseAppCheck = stub()

        @JvmStatic
        public actual fun getInstance(app: FirebaseApp): FirebaseAppCheck = stub()
    }
}
