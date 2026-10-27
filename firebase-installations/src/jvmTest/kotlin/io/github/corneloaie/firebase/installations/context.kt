package io.github.corneloaie.firebase.installations

import org.junit.Ignore

actual val context: Any = io.github.corneloaie.firebase.testContext

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class IgnoreForAndroidUnitTest

actual typealias IgnoreForJvm = Ignore
