/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.ai.internal

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlin.test.fail

/**
 * Asserts that [actual] is the JSON [expected], comparing numbers by value: a number's text differs between the
 * platforms (`-2.147483648E9` on Native, `-2147483648` on JS).
 */
internal fun assertJson(expected: String, actual: JsonElement) {
    val expectedJson = Json.parseToJsonElement(expected)
    if (!sameJson(expectedJson, actual)) fail("Expected JSON\n$expectedJson\nbut was\n$actual")
}

private fun sameJson(a: JsonElement, b: JsonElement): Boolean = when {
    a is JsonObject && b is JsonObject -> a.keys == b.keys && a.keys.all { sameJson(a.getValue(it), b.getValue(it)) }

    a is JsonArray && b is JsonArray -> a.size == b.size && a.indices.all { sameJson(a[it], b[it]) }

    a is JsonNull || b is JsonNull -> a == b

    a is JsonPrimitive && b is JsonPrimitive -> when {
        a.isString || b.isString -> a.isString == b.isString && a.content == b.content
        a.doubleOrNull != null && b.doubleOrNull != null -> a.doubleOrNull == b.doubleOrNull
        else -> a.content == b.content
    }

    else -> false
}
