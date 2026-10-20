/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.functions

/**
 * An element of a streamed call's response, as the Android SDK's `com.google.firebase.functions.StreamResponse`: a
 * [Message] for every chunk the function sends while it runs, then one [Result] with its return value. Plain common
 * code: on Android and the JVM it is a header stub for the SDK's own class. Common code receives them from
 * [streamAsFlow].
 */
public abstract class StreamResponse private constructor() {
    /** A chunk the function sent while running (`response.sendChunk(...)`), as the [message]'s `data`. */
    public class Message(public val message: HttpsCallableResult) : StreamResponse()

    /** The function's return value, as the [result]'s `data`: the last element of the stream. */
    public class Result(public val result: HttpsCallableResult) : StreamResponse()
}
