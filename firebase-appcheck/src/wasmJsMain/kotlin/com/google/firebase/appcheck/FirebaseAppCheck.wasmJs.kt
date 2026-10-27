/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory
import io.github.corneloaie.firebase.appcheck.ReCaptchaV3ProviderFactory
import io.github.corneloaie.firebase.appcheck.externals.AppCheck
import io.github.corneloaie.firebase.appcheck.externals.AppCheckTokenResult
import io.github.corneloaie.firebase.appcheck.externals.CustomProvider
import io.github.corneloaie.firebase.appcheck.externals.ReCaptchaEnterpriseProvider
import io.github.corneloaie.firebase.appcheck.externals.ReCaptchaV3Provider
import io.github.corneloaie.firebase.externals.errorCode
import io.github.corneloaie.firebase.externals.jsGet
import io.github.corneloaie.firebase.externals.json
import io.github.corneloaie.firebase.externals.toKotlin
import kotlin.js.JsException
import kotlin.js.Promise
import io.github.corneloaie.firebase.appcheck.externals.AppCheckProvider as JsAppCheckProvider
import io.github.corneloaie.firebase.appcheck.externals.getLimitedUseToken as jsGetLimitedUseToken
import io.github.corneloaie.firebase.appcheck.externals.getToken as jsGetToken
import io.github.corneloaie.firebase.appcheck.externals.initializeAppCheck as jsInitializeAppCheck
import io.github.corneloaie.firebase.appcheck.externals.onTokenChanged as jsOnTokenChanged
import io.github.corneloaie.firebase.appcheck.externals.setTokenAutoRefreshEnabled as jsSetTokenAutoRefreshEnabled

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

        public actual fun getInstance(app: FirebaseApp): FirebaseAppCheck = JsFirebaseAppCheck.of(app)
    }
}

/**
 * The [FirebaseAppCheck] of an app over the JS SDK, which starts App Check for an app once, with its provider
 * (`initializeAppCheck`): the first [installAppCheckProviderFactory] does that.
 *
 * A factory of Kotlin code is installed as a `CustomProvider` that asks the provider of the factory installed last,
 * so such factories can replace one another. The providers the JS SDK implements (reCAPTCHA, and the debug mode the
 * debug factory turns on) cannot be replaced, nor replace a provider.
 */
internal class JsFirebaseAppCheck private constructor(val app: FirebaseApp) : FirebaseAppCheck() {

    /** The JS SDK's App Check of the app, once a factory is installed. */
    var js: AppCheck? = null
        private set

    /** The key of the JS SDK provider App Check runs with; null for the provider of a factory of Kotlin code. */
    private var jsProviderKey: String? = null

    /** The provider of the installed factory of Kotlin code. */
    private var provider: AppCheckProvider? = null

    /** Each listener and, once App Check runs, the function that unsubscribes it. */
    private val listeners = mutableMapOf<AppCheckListener, (() -> Unit)?>()

    /** Automatic refresh follows `automaticDataCollectionEnabled`, as it follows the data collection setting on Android. */
    override fun installAppCheckProviderFactory(factory: AppCheckProviderFactory) {
        installAppCheckProviderFactory(factory, app.js.automaticDataCollectionEnabled)
    }

    /**
     * Starts App Check with [factory] the first time. Later, a factory of Kotlin code replaces the one installed before
     * it, and the factory App Check runs with only updates the refresh setting.
     *
     * @throws FirebaseException when the JS SDK would have to replace its provider: App Check started with a
     *   provider the JS SDK implements, or [factory] is one.
     */
    override fun installAppCheckProviderFactory(factory: AppCheckProviderFactory, isTokenAutoRefreshEnabled: Boolean) {
        val key = factory.jsProviderKey()
        val current = js
        if (current != null) {
            if (key != jsProviderKey) {
                throw FirebaseException("App Check of app ${app.name} already runs with another provider, which the Firebase JS SDK cannot replace")
            }
            if (key == null) provider = factory.create(app)
            rethrow { jsSetTokenAutoRefreshEnabled(current, isTokenAutoRefreshEnabled) }
            return
        }
        // The debug factory's provider only answers when the JS SDK is not in debug mode after all, with an explanation.
        val delegate = if (key == null || factory is DebugAppCheckProviderFactory) factory.create(app) else null
        if (factory is DebugAppCheckProviderFactory) enableDebugMode()
        val jsProvider = factory.jsProvider() ?: CustomProvider(customProviderOptions { delegatedToken() })
        val appCheck = rethrow { jsInitializeAppCheck(app.js, json("provider" to jsProvider, "isTokenAutoRefreshEnabled" to isTokenAutoRefreshEnabled)) }
        js = appCheck
        jsProviderKey = key
        provider = delegate
        listeners.keys.toList().forEach { listeners[it] = subscribe(appCheck, it) }
    }

    /** Does nothing before a factory is installed: installing it sets the refresh setting, as on Android. */
    override fun setTokenAutoRefreshEnabled(isTokenAutoRefreshEnabled: Boolean) {
        js?.let { rethrow { jsSetTokenAutoRefreshEnabled(it, isTokenAutoRefreshEnabled) } }
    }

    override fun getAppCheckToken(forceRefresh: Boolean): Task<AppCheckToken> {
        val appCheck = js ?: return notInstalled()
        return task { jsGetToken(appCheck, forceRefresh) }
    }

    /**
     * The JS SDK asks a custom provider for an ordinary token, so the provider of a factory of Kotlin code is asked for
     * its limited-use token directly, as App Check does on Android.
     */
    override fun getLimitedUseAppCheckToken(): Task<AppCheckToken> {
        val appCheck = js ?: return notInstalled()
        provider?.takeIf { jsProviderKey == null }?.let { return it.limitedUseToken() }
        return task { jsGetLimitedUseToken(appCheck) }
    }

    /**
     * Before a factory is installed the listener waits for App Check to start. As on Android, the JS SDK calls a new
     * listener with the current token when there is a valid one, though asynchronously.
     */
    override fun addAppCheckListener(listener: AppCheckListener) {
        if (listener in listeners) return
        listeners[listener] = js?.let { subscribe(it, listener) }
    }

    override fun removeAppCheckListener(listener: AppCheckListener) {
        listeners.remove(listener)?.invoke()
    }

    private fun subscribe(appCheck: AppCheck, listener: AppCheckListener): () -> Unit = rethrow {
        jsOnTokenChanged(appCheck) { listener.onAppCheckTokenChanged(JwtAppCheckToken(it.token)) }
    }

    /**
     * The token of the installed provider of Kotlin code, for the JS SDK's `CustomProvider`. A failure rejects the
     * promise with a reference to the Kotlin exception, which [toAppCheckException] unwraps again.
     */
    private fun delegatedToken(): Promise<JsAny?> = Promise { resolve, reject ->
        val current = provider
        if (current == null) {
            reject(FirebaseException("No AppCheckProvider installed.").toJsReference())
            return@Promise
        }
        val task = try {
            current.getToken()
        } catch (e: Throwable) {
            reject(e.toJsReference())
            return@Promise
        }
        task.addOnCompleteListener { completed ->
            val token = if (completed.isSuccessful) completed.result else null
            if (token != null) {
                resolve(json("token" to token.token, "expireTimeMillis" to token.expireTimeMillis.toDouble()))
            } else {
                reject((completed.exception ?: FirebaseException("The App Check token request was canceled")).toJsReference())
            }
        }
    }

    override fun toString(): String = "FirebaseAppCheck(app=${app.name})"

    companion object {
        private val instances = mutableMapOf<String, JsFirebaseAppCheck>()

        /** The instance of [app], one per app as on Android: the JS SDK's App Check starts once per app. */
        fun of(app: FirebaseApp): JsFirebaseAppCheck = instances[app.name]?.takeIf { it.app == app } ?: JsFirebaseAppCheck(app).also { instances[app.name] = it }
    }
}

/** The key of the JS SDK provider of a factory the JS SDK implements: the same key is the same provider. */
private fun AppCheckProviderFactory.jsProviderKey(): String? = when (this) {
    is DebugAppCheckProviderFactory -> "debug"
    is RecaptchaAppCheckProviderFactory -> "recaptcha-enterprise:$siteKey"
    is ReCaptchaV3ProviderFactory -> "recaptcha-v3:$siteKey"
    else -> null
}

/** The JS SDK provider of a factory the JS SDK implements; null for the debug factory, which is a mode of the JS SDK. */
private fun AppCheckProviderFactory.jsProvider(): JsAppCheckProvider? = when (this) {
    is RecaptchaAppCheckProviderFactory -> ReCaptchaEnterpriseProvider(siteKey)
    is ReCaptchaV3ProviderFactory -> ReCaptchaV3Provider(siteKey)
    else -> null
}

/** The options of a `CustomProvider` whose `getToken` is [getToken]. */
private fun customProviderOptions(getToken: () -> Promise<JsAny?>): JsAny = js("({ getToken: getToken })")

/** Turns on the JS SDK's debug mode, unless the page set a debug token itself. The JS SDK reads it when App Check first starts. */
private fun enableDebugMode(): Unit = js("{ if (typeof globalThis.FIREBASE_APPCHECK_DEBUG_TOKEN !== 'string') globalThis.FIREBASE_APPCHECK_DEBUG_TOKEN = true; }")

private fun AppCheckProvider.limitedUseToken(): Task<AppCheckToken> = try {
    getLimitedUseToken()
} catch (e: Exception) {
    TaskCompletionSource<AppCheckToken>().apply { setException(e) }.task
}

private fun notInstalled(): Task<AppCheckToken> = TaskCompletionSource<AppCheckToken>().apply { setException(FirebaseException("No AppCheckProvider installed.")) }.task

private inline fun task(request: () -> Promise<AppCheckTokenResult>): Task<AppCheckToken> {
    val source = TaskCompletionSource<AppCheckToken>()
    try {
        request().then(
            onFulfilled = {
                source.trySetResult(JwtAppCheckToken(it.token))
                null
            },
            onRejected = {
                source.trySetException(it.toAppCheckException())
                null
            },
        )
    } catch (e: Throwable) {
        source.trySetException(e.toAppCheckException())
    }
    return source.task
}

private inline fun <R> rethrow(function: () -> R): R = try {
    function()
} catch (e: Throwable) {
    throw e.toAppCheckException()
}

/** The JS SDK's errors carry an `appCheck/...` code; the exception a provider of Kotlin code failed with is kept, as on Android. */
private fun Throwable.toAppCheckException(): Exception = when (this) {
    is JsException -> appCheckException(errorCode(), this)
    is Exception -> this
    else -> FirebaseException(message ?: "App Check request failed")
}

/**
 * A promise rejection: a reference to the Kotlin exception a provider of Kotlin code failed with, which
 * [toThrowableOrNull] returns as it is, or a JS SDK error, which it returns as a [JsException] (not an [Exception]).
 */
private fun JsAny.toAppCheckException(): Exception {
    val thrown = toThrowableOrNull()
    if (thrown is Exception) return thrown
    return appCheckException(jsGet(this, "code").toKotlin()?.toString(), thrown ?: Exception(jsGet(this, "message").toKotlin()?.toString()))
}

private fun appCheckException(code: String?, cause: Throwable): Exception = when (code) {
    "appCheck/fetch-network-error" -> FirebaseNetworkException(code, cause)
    "appCheck/throttled", "appCheck/initial-throttle" -> FirebaseTooManyRequestsException(code, cause)
    null -> FirebaseException("App Check request failed", cause)
    else -> FirebaseException(code, cause)
}
