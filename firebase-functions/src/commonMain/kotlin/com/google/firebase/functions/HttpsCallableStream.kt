/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("HttpsCallableStreamKt")

package com.google.firebase.functions

import kotlinx.coroutines.flow.Flow
import kotlin.jvm.JvmName

/**
 * Calls the function with [data] and streams its response: a [StreamResponse.Message] for every chunk the function
 * sends while it runs, then a [StreamResponse.Result] with its return value, after which the flow completes. A failed
 * call fails the flow with a [FirebaseFunctionsException].
 *
 * The multiplatform counterpart of the Android SDK's `stream(data)`, whose `org.reactivestreams.Publisher` common code
 * cannot name; shipped on Android and the JVM too (see keepClasses in the build file). Not available on Apple
 * platforms, where the iOS SDK's streaming API is Swift-only, or on the JVM, whose firebase-java-sdk has no streaming:
 * there the flow fails with an [UnsupportedOperationException].
 */
public expect fun HttpsCallableReference.streamAsFlow(data: Any? = null): Flow<StreamResponse>
