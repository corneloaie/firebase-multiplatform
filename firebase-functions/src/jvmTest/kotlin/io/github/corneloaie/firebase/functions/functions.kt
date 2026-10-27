/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.functions

import io.github.corneloaie.firebase.testContext

actual val emulatorHost: String = "localhost"
actual val context: Any = testContext

@Suppress("UNCHECKED_CAST")
actual fun detailValue(details: Any?, key: String): Any? = (details as Map<Any?, Any?>)[key]

actual annotation class IgnoreForAndroidUnitTest

actual val streamingSupported: Boolean = true
