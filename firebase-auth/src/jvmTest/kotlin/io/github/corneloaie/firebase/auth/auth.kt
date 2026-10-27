/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("tests")

package io.github.corneloaie.firebase.auth

import io.github.corneloaie.firebase.testContext

actual val emulatorHost: String = "10.0.2.2"

actual val context: Any = testContext

actual val supportsPlayGamesSignIn: Boolean = false // firebase-java-sdk has no Play Games sign-in

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class IgnoreForAndroidUnitTest
