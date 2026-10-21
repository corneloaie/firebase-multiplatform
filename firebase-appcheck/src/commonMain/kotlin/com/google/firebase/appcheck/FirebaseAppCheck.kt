/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp

/**
 * Firebase App Check, mirroring `com.google.firebase.appcheck.FirebaseAppCheck` from the Firebase Android SDK. It
 * attests that requests to Firebase backends come from your app, and gives you tokens for your own backend.
 *
 * Tokens come from the [AppCheckProvider] of the installed [AppCheckProviderFactory]. That factory can be
 * [com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory],
 * [com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory] (Android only),
 * [com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory], or a factory of your own.
 *
 * The platform SDKs install a factory differently:
 * - **Android:** [installAppCheckProviderFactory] replaces the provider of this app at any time.
 * - **Apple:** the iOS SDK keeps one factory for the whole process. It reads that factory when an app is configured,
 *   and creates the app's App Check then. [installAppCheckProviderFactory] sets the factory, so it only applies to apps
 *   configured afterwards. For the default app, set it before `Firebase.initialize` from Apple code with
 *   `FirebaseAppCheck.setAppCheckProviderFactory`. Until then the iOS SDK's default applies: the debug provider on a
 *   simulator, DeviceCheck on a device.
 * - **JS:** the JS SDK starts App Check for an app once, with its provider (`initializeAppCheck`). The first
 *   [installAppCheckProviderFactory] does that, and the instance has no token before. Afterwards a factory of your
 *   own can still replace another one of your own. The JS SDK's own providers (the debug provider and reCAPTCHA)
 *   can neither replace a provider nor be replaced: [installAppCheckProviderFactory] throws then.
 */
public expect abstract class FirebaseAppCheck() {
    /**
     * Installs [factory], replacing the factory installed before. Automatic token refresh follows the app's data
     * collection setting (the iOS SDK keeps its own default there). See the class documentation for when the factory
     * takes effect on each platform.
     */
    public abstract fun installAppCheckProviderFactory(factory: AppCheckProviderFactory)

    /**
     * Installs [factory], replacing the factory installed before, and sets whether App Check refreshes its token
     * automatically. See the class documentation for when the factory takes effect on each platform.
     */
    public abstract fun installAppCheckProviderFactory(factory: AppCheckProviderFactory, isTokenAutoRefreshEnabled: Boolean)

    /** Sets whether App Check refreshes its token automatically before it expires. */
    public abstract fun setTokenAutoRefreshEnabled(isTokenAutoRefreshEnabled: Boolean)

    /**
     * Requests an App Check token. Use it only for requests to your own backend: requests to Firebase backends get a
     * token automatically.
     *
     * @param forceRefresh whether to request a new token even when the cached one is still valid. Pass `true` only
     *   when your backend rejected the cached token.
     */
    public abstract fun getAppCheckToken(forceRefresh: Boolean): Task<AppCheckToken>

    /**
     * Requests a limited-use App Check token, for a backend with replay protection. It does not replace the token
     * that [getAppCheckToken] caches.
     */
    public abstract fun getLimitedUseAppCheckToken(): Task<AppCheckToken>

    /**
     * Registers [listener] for new App Check tokens. It is not called when a token expires. On Android and JS it is
     * also called right away with the cached token when that is still valid; the iOS SDK reports new tokens only.
     */
    public abstract fun addAppCheckListener(listener: AppCheckListener)

    /** Unregisters a [listener] registered with [addAppCheckListener]. */
    public abstract fun removeAppCheckListener(listener: AppCheckListener)

    /** Receives each new App Check token, mirroring `com.google.firebase.appcheck.FirebaseAppCheck.AppCheckListener`. */
    public fun interface AppCheckListener {
        /**
         * Called with each new [token], on the main thread. The call with the cached token that registering the
         * listener makes on Android runs on the registering thread.
         */
        public fun onAppCheckTokenChanged(token: AppCheckToken)
    }

    public companion object {
        /** Returns the [FirebaseAppCheck] of the default [FirebaseApp]. */
        public fun getInstance(): FirebaseAppCheck

        /** Returns the [FirebaseAppCheck] of [app]. */
        public fun getInstance(app: FirebaseApp): FirebaseAppCheck
    }
}
