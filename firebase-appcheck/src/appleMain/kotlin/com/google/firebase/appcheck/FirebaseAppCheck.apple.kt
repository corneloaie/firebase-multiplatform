/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck

import swiftPMImport.dev.gitlive.firebase.appcheck.FIRAppCheck
import swiftPMImport.dev.gitlive.firebase.appcheck.FIRAppCheckAppCheckTokenDidChangeNotification
import swiftPMImport.dev.gitlive.firebase.appcheck.kFIRAppCheckAppNameNotificationKey
import swiftPMImport.dev.gitlive.firebase.appcheck.kFIRAppCheckTokenNotificationKey
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObjectProtocol
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

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
        public actual fun getInstance(): FirebaseAppCheck = getInstance(FirebaseApp.getInstance())

        public actual fun getInstance(app: FirebaseApp): FirebaseAppCheck = IosFirebaseAppCheck.of(app)

        /**
         * Apple only: sets the provider factory of the apps configured from now on, the iOS SDK's
         * `AppCheck.setAppCheckProviderFactory`. The iOS SDK reads it when an app is configured, so call this before
         * `Firebase.initialize` to choose the provider of the default app. `null` leaves apps configured afterwards
         * without App Check.
         */
        public fun setAppCheckProviderFactory(factory: AppCheckProviderFactory?) {
            FIRAppCheck.setAppCheckProviderFactory(factory?.toIos())
        }
    }
}

/**
 * The [FirebaseAppCheck] of an app, over the iOS SDK's `FIRAppCheck`. The iOS SDK creates that when the app is
 * configured, with the provider factory set at that time, so a factory installed here applies to apps configured later.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class IosFirebaseAppCheck private constructor(val app: FirebaseApp) : FirebaseAppCheck() {

    /** The iOS SDK's App Check of the app; null when there was no provider factory when the app was configured. */
    val ios: FIRAppCheck? get() = FIRAppCheck.appCheckWithApp(app.ios as objcnames.classes.FIRApp)

    private val observers = AtomicReference<Map<AppCheckListener, NSObjectProtocol>>(emptyMap())

    /** Sets the iOS SDK's provider factory, which applies to the apps configured from now on. */
    override fun installAppCheckProviderFactory(factory: AppCheckProviderFactory) {
        FIRAppCheck.setAppCheckProviderFactory(factory.toIos())
    }

    override fun installAppCheckProviderFactory(factory: AppCheckProviderFactory, isTokenAutoRefreshEnabled: Boolean) {
        installAppCheckProviderFactory(factory)
        setTokenAutoRefreshEnabled(isTokenAutoRefreshEnabled)
    }

    /** The iOS SDK keeps the setting for the next launches too. Does nothing when the app has no App Check. */
    override fun setTokenAutoRefreshEnabled(isTokenAutoRefreshEnabled: Boolean) {
        ios?.setIsTokenAutoRefreshEnabled(isTokenAutoRefreshEnabled)
    }

    override fun getAppCheckToken(forceRefresh: Boolean): Task<AppCheckToken> = withAppCheck { appCheck, completion ->
        appCheck.tokenForcingRefresh(forceRefresh, completion)
    }

    override fun getLimitedUseAppCheckToken(): Task<AppCheckToken> = withAppCheck { appCheck, completion ->
        appCheck.limitedUseTokenWithCompletion(completion)
    }

    /**
     * Observes the iOS SDK's token-changed notification for this app, on the main queue. Unlike on Android, a new
     * listener is not called with the cached token. The notification carries the token alone, so the expiry is read
     * from the token.
     */
    override fun addAppCheckListener(listener: AppCheckListener) {
        val center = NSNotificationCenter.defaultCenter
        val observer = center.addObserverForName(FIRAppCheckAppCheckTokenDidChangeNotification, null, NSOperationQueue.mainQueue) { notification ->
            val info = notification?.userInfo
            if (info?.get(kFIRAppCheckAppNameNotificationKey) == app.name) {
                (info?.get(kFIRAppCheckTokenNotificationKey) as? String)?.let { listener.onAppCheckTokenChanged(JwtAppCheckToken(it)) }
            }
        }
        while (true) {
            val current = observers.load()
            val previous = current[listener]
            if (observers.compareAndSet(current, current + (listener to observer))) {
                previous?.let(center::removeObserver)
                return
            }
        }
    }

    override fun removeAppCheckListener(listener: AppCheckListener) {
        while (true) {
            val current = observers.load()
            val observer = current[listener] ?: return
            if (observers.compareAndSet(current, current - listener)) {
                NSNotificationCenter.defaultCenter.removeObserver(observer)
                return
            }
        }
    }

    private inline fun withAppCheck(start: (FIRAppCheck, IosTokenCompletion) -> Unit): Task<AppCheckToken> {
        val appCheck = ios ?: return TaskCompletionSource<AppCheckToken>().apply {
            setException(FirebaseException("No App Check for app ${app.name}: the iOS SDK had no provider factory when the app was configured"))
        }.task
        return iosTokenTask { completion -> start(appCheck, completion) }
    }

    override fun toString(): String = "FirebaseAppCheck(app=${app.name})"

    companion object {
        private val instances = AtomicReference<Map<String, IosFirebaseAppCheck>>(emptyMap())

        /** The instance of [app], one per app as on Android, so that listeners can be removed through any reference. */
        fun of(app: FirebaseApp): IosFirebaseAppCheck {
            while (true) {
                val current = instances.load()
                current[app.name]?.takeIf { it.app == app }?.let { return it }
                val created = IosFirebaseAppCheck(app)
                if (instances.compareAndSet(current, current + (app.name to created))) return created
            }
        }
    }
}
