/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.functions

actual val emulatorHost: String = "localhost"
actual val context: Any = Unit

actual fun detailValue(details: Any?, key: String): Any? = details.asDynamic()[key]

actual annotation class IgnoreForAndroidUnitTest

actual val streamingSupported: Boolean = true
