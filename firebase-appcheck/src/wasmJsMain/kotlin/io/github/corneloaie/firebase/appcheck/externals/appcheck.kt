@file:JsModule("firebase/app-check")

package io.github.corneloaie.firebase.appcheck.externals

import io.github.corneloaie.firebase.externals.FirebaseApp
import kotlin.js.Promise

/** The App Check of an app, which [initializeAppCheck] starts. */
public external interface AppCheck : JsAny {
    public val app: FirebaseApp
}

/** A token as the JS SDK reports it: the JWT alone. */
public external interface AppCheckTokenResult : JsAny {
    public val token: String
}

/** An App Check provider, for the `provider` of the options of [initializeAppCheck]. */
public external interface AppCheckProvider : JsAny

/** A provider whose `getToken` option returns a promise of `{ token, expireTimeMillis }`. */
public external class CustomProvider(options: JsAny) : AppCheckProvider

/** The reCAPTCHA v3 provider, for a reCAPTCHA v3 site key. */
public external class ReCaptchaV3Provider(siteKey: String) : AppCheckProvider

/** The reCAPTCHA Enterprise provider, for a score-based reCAPTCHA Enterprise site key. */
public external class ReCaptchaEnterpriseProvider(siteKey: String) : AppCheckProvider

/** Starts App Check for [app] with `{ provider, isTokenAutoRefreshEnabled }`; it can only start once per app. */
public external fun initializeAppCheck(app: FirebaseApp?, options: JsAny): AppCheck

public external fun getToken(appCheckInstance: AppCheck, forceRefresh: Boolean): Promise<AppCheckTokenResult>

public external fun getLimitedUseToken(appCheckInstance: AppCheck): Promise<AppCheckTokenResult>

/** Calls [onNext] with each new token; the returned function unsubscribes. */
public external fun onTokenChanged(appCheckInstance: AppCheck, onNext: (AppCheckTokenResult) -> Unit): () -> Unit

public external fun setTokenAutoRefreshEnabled(appCheckInstance: AppCheck, isTokenAutoRefreshEnabled: Boolean)
