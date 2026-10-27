@file:JsModule("firebase/messaging")

package io.github.corneloaie.firebase.messaging.externals

import io.github.corneloaie.firebase.externals.FirebaseApp
import kotlin.js.Promise

public external fun getMessaging(
    app: FirebaseApp? = definedExternally,
): Messaging

public external fun getToken(messaging: Messaging = definedExternally, options: JsAny? = definedExternally): Promise<JsString>

public external fun deleteToken(messaging: Messaging = definedExternally): Promise<JsBoolean>

public external interface Messaging : JsAny
