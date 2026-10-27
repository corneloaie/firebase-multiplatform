@file:JsModule("firebase/analytics")

package io.github.corneloaie.firebase.analytics.externals

import io.github.corneloaie.firebase.externals.FirebaseApp
import kotlin.js.Promise

public external fun getAnalytics(app: FirebaseApp? = definedExternally): FirebaseAnalytics

public external fun logEvent(analytics: FirebaseAnalytics, name: String, parameters: JsAny?)
public external fun setUserProperties(analytics: FirebaseAnalytics, properties: JsAny)
public external fun setUserId(analytics: FirebaseAnalytics, id: String?)
public external fun setDefaultEventParameters(parameters: JsAny?)
public external fun setAnalyticsCollectionEnabled(analytics: FirebaseAnalytics, enabled: Boolean)
public external fun getGoogleAnalyticsClientId(analytics: FirebaseAnalytics): Promise<JsString>

/** Takes a `ConsentSettings` object: `ad_storage`, `analytics_storage`, ... set to `granted` or `denied`. */
public external fun setConsent(consentSettings: JsAny)

public external interface FirebaseAnalytics : JsAny {
    public val app: FirebaseApp
}
