/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.internal

import io.github.corneloaie.firebase.externals.asJsAny
import io.github.corneloaie.firebase.externals.jsGet
import io.github.corneloaie.firebase.externals.jsIsArray
import io.github.corneloaie.firebase.externals.jsObjectKeys
import io.github.corneloaie.firebase.externals.json
import io.github.corneloaie.firebase.externals.toKotlin

public val EncodedObject.js: JsAny get() = json(*getRaw().entries.map { (key, value) -> key to value }.toTypedArray())

@PublishedApi
internal actual fun Any.asNativeMap(): Map<*, *>? {
    val obj: JsAny = when (this) {
        is Number -> return null
        is Boolean -> return null
        is String -> return null
        is Map<*, *> -> {
            if (keys.all { it is String }) {
                json(*entries.map { (key, value) -> (key as String) to value }.toTypedArray())
            } else {
                return null
            }
        }
        is Collection<*> -> return null
        is Array<*> -> return null
        else -> asJsAny().takeUnless { jsIsArray(it) } ?: return null
    }
    val mutableMap = mutableMapOf<String, Any?>()
    val keys = jsObjectKeys(obj)
    for (index in 0 until keys.length) {
        val key = keys[index].toKotlin() as String
        mutableMap[key] = jsGet(obj, key).toKotlin()
    }
    return mutableMap.toMap()
}
