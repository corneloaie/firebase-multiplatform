/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.functions

import io.github.corneloaie.firebase.externals.asJsAny
import io.github.corneloaie.firebase.externals.jsGet
import io.github.corneloaie.firebase.externals.toKotlin

actual val emulatorHost: String = "localhost"
actual val context: Any = Unit

actual fun detailValue(details: Any?, key: String): Any? = details?.let { jsGet(it.asJsAny(), key) }?.toKotlin()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class IgnoreForAndroidUnitTest

actual val streamingSupported: Boolean = true
