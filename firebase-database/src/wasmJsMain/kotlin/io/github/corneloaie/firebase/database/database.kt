/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.database

import com.google.firebase.database.objectKeys
import io.github.corneloaie.firebase.database.externals.Database
import io.github.corneloaie.firebase.externals.jsGet
import io.github.corneloaie.firebase.externals.toJs
import io.github.corneloaie.firebase.externals.toKotlin
import io.github.corneloaie.firebase.internal.EncodedObject
import io.github.corneloaie.firebase.internal.js
import com.google.firebase.database.DataSnapshot as CompatDataSnapshot
import com.google.firebase.database.MutableData as CompatMutableData
import io.github.corneloaie.firebase.database.externals.DataSnapshot as JsDataSnapshot
import io.github.corneloaie.firebase.database.externals.OnDisconnect as JsOnDisconnect
import io.github.corneloaie.firebase.database.externals.Query as JsQuery

/** The underlying Firebase JS SDK object. */
public val FirebaseDatabase.js: Database get() = compat.js

/** The underlying Firebase JS SDK object. */
public val Query.js: JsQuery get() = compat.js

/** The underlying Firebase JS SDK object. */
public val DataSnapshot.js: JsDataSnapshot get() = compat.js

/** The underlying Firebase JS SDK object. */
public val OnDisconnect.js: JsOnDisconnect get() = compat.js

/** The underlying Firebase JS SDK database. */
public val Query.database: Database get() = compat.jsDatabase

/** The underlying Firebase JS SDK database. */
public val DataSnapshot.database: Database get() = compat.database

/** The underlying Firebase JS SDK database. */
public val OnDisconnect.database: Database get() = compat.ref.jsDatabase

internal actual fun EncodedObject.toCompatMap(): Map<String, Any?> = js.let { json -> objectKeys(json).associateWith { jsGet(json, it) } }

/** The wasm decoders take JS objects and arrays as they are, but JS primitives as their Kotlin equivalents. */
internal actual val CompatDataSnapshot.nativeValue: Any? get() = js.`val`().toKotlin()

/** As [CompatDataSnapshot.nativeValue], JS primitives are read as their Kotlin equivalents for the wasm decoders. */
internal actual var CompatMutableData.nativeValue: Any?
    get() = jsValue.toKotlin()
    set(value) {
        jsValue = value.toJs()
    }
