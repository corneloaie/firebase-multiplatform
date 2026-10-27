/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.perf

import com.google.firebase.perf.metrics.Trace
import io.github.corneloaie.firebase.externals.errorCode
import io.github.corneloaie.firebase.perf.FirebasePerformanceException
import io.github.corneloaie.firebase.perf.externals.getPerformance
import io.github.corneloaie.firebase.perf.externals.trace
import io.github.corneloaie.firebase.perf.externals.FirebasePerformance as JsFirebasePerformance
import kotlin.js.JsException

/** @property js The underlying Firebase JS SDK object. */
public actual class FirebasePerformance internal constructor(public val js: JsFirebasePerformance) {
    public actual var isPerformanceCollectionEnabled: Boolean
        get() = js.dataCollectionEnabled
        set(value) {
            js.dataCollectionEnabled = value
        }

    public actual fun newTrace(traceName: String): Trace = rethrow { Trace(trace(js, traceName)) }

    override fun equals(other: Any?): Boolean = other is FirebasePerformance && other.js == js

    override fun hashCode(): Int = js.hashCode()

    override fun toString(): String = "FirebasePerformance($js)"

    @Retention(AnnotationRetention.SOURCE)
    public actual annotation class HttpMethod {
        public actual companion object {
            public actual val CONNECT: String = "CONNECT"
            public actual val DELETE: String = "DELETE"
            public actual val GET: String = "GET"
            public actual val HEAD: String = "HEAD"
            public actual val OPTIONS: String = "OPTIONS"
            public actual val PATCH: String = "PATCH"
            public actual val POST: String = "POST"
            public actual val PUT: String = "PUT"
            public actual val TRACE: String = "TRACE"
        }
    }

    public actual companion object {
        /** Maximum length of a trace name. */
        public actual val MAX_TRACE_NAME_LENGTH: Int = 100

        public actual fun getInstance(): FirebasePerformance = rethrow { FirebasePerformance(getPerformance()) }

        public actual fun startTrace(traceName: String): Trace = getInstance().newTrace(traceName).apply { start() }
    }
}

internal inline fun <R> rethrow(function: () -> R): R {
    try {
        return function()
    } catch (e: JsException) {
        throw errorToException(e)
    }
}

internal fun errorToException(error: JsException): FirebasePerformanceException = (error.errorCode() ?: error.message ?: "")
    .lowercase()
    .let { code -> FirebasePerformanceException(code, error) }
