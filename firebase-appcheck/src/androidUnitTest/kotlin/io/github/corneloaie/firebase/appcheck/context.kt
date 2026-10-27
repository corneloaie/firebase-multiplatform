package io.github.corneloaie.firebase.appcheck

import org.junit.Ignore

actual val context: Any = ""

actual typealias IgnoreForAndroidUnitTest = Ignore

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class IgnoreForApple
