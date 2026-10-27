package io.github.corneloaie.firebase.appcheck

import kotlin.test.Ignore

actual val context: Any = Unit

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class IgnoreForAndroidUnitTest

actual typealias IgnoreForApple = Ignore
