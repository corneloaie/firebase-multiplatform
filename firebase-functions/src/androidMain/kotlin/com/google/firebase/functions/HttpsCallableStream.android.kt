/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("HttpsCallableStreamKt")

package com.google.firebase.functions

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/*
 * Unlike the header stubs around it, this file is shipped (see keepClasses in the build file): it adapts the SDK's
 * org.reactivestreams.Publisher to a Flow. firebase-java-sdk before 0.7.0 has no stream(), so there the call fails.
 */

public actual fun HttpsCallableReference.streamAsFlow(data: Any?): Flow<StreamResponse> = callbackFlow {
    val publisher = try {
        stream(data)
    } catch (e: NoSuchMethodError) {
        throw UnsupportedOperationException("Streaming callable functions is not provided by this Firebase SDK", e)
    }
    var subscription: Subscription? = null
    publisher.subscribe(
        object : Subscriber<StreamResponse> {
            override fun onSubscribe(s: Subscription) {
                subscription = s
                s.request(Long.MAX_VALUE)
            }

            override fun onNext(response: StreamResponse) {
                trySendBlocking(response)
            }

            override fun onError(t: Throwable) {
                close(t)
            }

            override fun onComplete() {
                close()
            }
        },
    )
    awaitClose { subscription?.cancel() }
}
