@file:JsModule("firebase/app-check")
@file:JsNonModule

package io.github.corneloaie.firebase.appcheck.externals

import io.github.corneloaie.firebase.externals.FirebaseApp
import kotlin.js.Json
import kotlin.js.Promise

/** The App Check of an app, which [initializeAppCheck] starts. */
public external interface AppCheck {
    public val app: FirebaseApp
}

/** A token as the JS SDK reports it: the JWT alone. */
public external interface AppCheckTokenResult {
    public val token: String
}

/** An App Check provider, for the `provider` of the options of [initializeAppCheck]. */
public external interface AppCheckProvider

/** A provider whose `getToken` option returns a promise of `{ token, expireTimeMillis }`. */
public external class CustomProvider(options: Json) : AppCheckProvider

/** The reCAPTCHA v3 provider, for a reCAPTCHA v3 site key. */
public external class ReCaptchaV3Provider(siteKey: String) : AppCheckProvider

/** The reCAPTCHA Enterprise provider, for a score-based reCAPTCHA Enterprise site key. */
public external class ReCaptchaEnterpriseProvider(siteKey: String) : AppCheckProvider

/** Starts App Check for [app] with `{ provider, isTokenAutoRefreshEnabled }`; it can only start once per app. */
public external fun initializeAppCheck(app: FirebaseApp?, options: Json): AppCheck

public external fun getToken(appCheckInstance: AppCheck, forceRefresh: Boolean): Promise<AppCheckTokenResult>

public external fun getLimitedUseToken(appCheckInstance: AppCheck): Promise<AppCheckTokenResult>

/** Calls [onNext] with each new token; the returned function unsubscribes. */
public external fun onTokenChanged(appCheckInstance: AppCheck, onNext: (AppCheckTokenResult) -> Unit): () -> Unit

public external fun setTokenAutoRefreshEnabled(appCheckInstance: AppCheck, isTokenAutoRefreshEnabled: Boolean)
