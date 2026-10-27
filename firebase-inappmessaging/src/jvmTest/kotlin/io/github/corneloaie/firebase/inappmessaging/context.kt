/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.inappmessaging

import io.github.corneloaie.firebase.testContext

actual val context: Any = testContext

internal actual fun <T> onAppThread(block: () -> T): T = block()

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class IgnoreForAndroidUnitTest
