/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.database

import io.github.corneloaie.firebase.externals.asJsArray
import io.github.corneloaie.firebase.externals.jsGet
import io.github.corneloaie.firebase.externals.jsIsArray
import io.github.corneloaie.firebase.externals.jsObjectKeys
import io.github.corneloaie.firebase.externals.jsTypeOf
import io.github.corneloaie.firebase.externals.toKotlin
import io.github.corneloaie.firebase.externals.toKotlinString
import io.github.corneloaie.firebase.externals.toList
import kotlin.math.abs
import kotlin.math.floor

private const val MAX_EXACT_INTEGER = 9007199254740992.0

/*
 * The JS SDK reads and writes plain JS values; the layer presents them as the natural Kotlin types the Android SDK
 * uses (Map, List, String, Boolean, Long, Double), integral numbers reading as Long like the Android SDK's. Kotlin values
 * are written with the shared `toJs()` (io.github.corneloaie.firebase.externals), which converts them as the JS layer does: maps
 * become objects, collections arrays, longs numbers.
 */

/** A plain JS value as its natural Kotlin type. */
internal fun JsAny?.fromJs(): Any? {
    if (this == null) return null
    return when (jsTypeOf(this)) {
        "string", "boolean" -> toKotlin()

        "number" -> {
            val number = (toKotlin() as Number).toDouble()
            if (number == floor(number) && abs(number) < MAX_EXACT_INTEGER) number.toLong() else number
        }

        "object" -> if (jsIsArray(this)) {
            asJsArray().toList().map { it.fromJs() }
        } else {
            objectKeys(this).associateWith { key -> jsGet(this, key).fromJs() }
        }

        else -> this
    }
}

/** `Object.keys(value)` as Kotlin strings. */
internal fun objectKeys(value: JsAny): List<String> = jsObjectKeys(value).toList().map { it.toKotlinString() }
