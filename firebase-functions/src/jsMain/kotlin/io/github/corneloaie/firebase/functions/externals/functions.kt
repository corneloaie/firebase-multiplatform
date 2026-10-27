@file:JsModule("firebase/functions")
@file:JsNonModule

package io.github.corneloaie.firebase.functions.externals

import io.github.corneloaie.firebase.externals.FirebaseApp
import kotlin.js.Json

public external fun connectFunctionsEmulator(functions: Functions, host: String, port: Int)

public external fun getFunctions(
    app: FirebaseApp? = definedExternally,
    regionOrCustomDomain: String? = definedExternally,
): Functions

public external fun httpsCallable(functions: Functions, name: String, options: Json?): HttpsCallable

public external fun httpsCallableFromURL(functions: Functions, url: String, options: Json?): HttpsCallable

public external interface Functions

public external interface HttpsCallableResult {
    public val data: Any?
}

public external interface HttpsCallable
