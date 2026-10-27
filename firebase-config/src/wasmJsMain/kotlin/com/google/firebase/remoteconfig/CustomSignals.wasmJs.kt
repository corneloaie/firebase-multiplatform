/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.remoteconfig

import io.github.corneloaie.firebase.externals.jsGet
import io.github.corneloaie.firebase.externals.jsObject
import io.github.corneloaie.firebase.externals.jsObjectKeys
import io.github.corneloaie.firebase.externals.jsSet
import io.github.corneloaie.firebase.externals.jsonStringify
import io.github.corneloaie.firebase.externals.toJs
import io.github.corneloaie.firebase.externals.toKotlinString
import io.github.corneloaie.firebase.externals.toList

/** @property js The signals as the JS SDK takes them: a string, number or null (removes the signal) per key. */
public actual class CustomSignals internal constructor(public val js: JsAny) {
    override fun equals(other: Any?): Boolean = other is CustomSignals && jsonStringify(other.js) == jsonStringify(js)

    override fun hashCode(): Int = jsonStringify(js).hashCode()

    override fun toString(): String = "CustomSignals(${jsonStringify(js)})"

    public actual class Builder actual constructor() {
        private val signals = jsObject()

        public actual fun put(key: String, value: String?): Builder = apply { jsSet(signals, key, value.toJs()) }

        public actual fun put(key: String, value: Long): Builder = apply { jsSet(signals, key, value.toDouble().toJs()) }

        public actual fun put(key: String, value: Double): Builder = apply { jsSet(signals, key, value.toJs()) }

        public actual fun build(): CustomSignals = CustomSignals(
            jsObject().also { copy -> jsObjectKeys(signals).toList().forEach { key -> jsSet(copy, key.toKotlinString(), jsGet(signals, key.toKotlinString())) } },
        )
    }
}
