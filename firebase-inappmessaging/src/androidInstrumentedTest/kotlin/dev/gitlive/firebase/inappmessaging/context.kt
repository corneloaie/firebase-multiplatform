/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("tests")

package dev.gitlive.firebase.inappmessaging

import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.inappmessaging.FirebaseInAppMessaging

actual val context: Any = InstrumentationRegistry.getInstrumentation().targetContext

/**
 * The Android SDK creates a Handler on the thread that first gets the instance, so that thread needs a Looper. Apps get
 * it on the main thread at startup; the test runner's thread has none.
 */
internal actual fun createInAppMessaging() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync { FirebaseInAppMessaging.getInstance() }
}

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class IgnoreForAndroidUnitTest
