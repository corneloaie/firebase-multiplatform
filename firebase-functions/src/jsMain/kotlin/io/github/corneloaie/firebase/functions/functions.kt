/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.functions

import io.github.corneloaie.firebase.functions.externals.Functions
import io.github.corneloaie.firebase.functions.externals.HttpsCallable
import io.github.corneloaie.firebase.functions.externals.HttpsCallableResult as JsHttpsCallableResult

/** The underlying Firebase JS SDK object. */
public val FirebaseFunctions.js: Functions get() = compat.js

/** The underlying Firebase JS SDK callable, created with the reference's current timeout. */
public val HttpsCallableReference.js: HttpsCallable get() = compat.js

/** The underlying Firebase JS SDK object. */
public val HttpsCallableResult.js: JsHttpsCallableResult get() = compat.js
