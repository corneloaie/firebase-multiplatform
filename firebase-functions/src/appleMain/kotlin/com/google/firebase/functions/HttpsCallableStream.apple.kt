/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.functions

import dev.gitlive.firebase.UnsupportedOnApple
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Fails: the iOS SDK's streaming API (`Callable.stream`) is Swift-only, which Kotlin/Native cannot call. */
@UnsupportedOnApple
public actual fun HttpsCallableReference.streamAsFlow(data: Any?): Flow<StreamResponse> = flow {
    throw UnsupportedOperationException("Streaming callable functions is not provided by the Firebase iOS SDK to Objective-C")
}
