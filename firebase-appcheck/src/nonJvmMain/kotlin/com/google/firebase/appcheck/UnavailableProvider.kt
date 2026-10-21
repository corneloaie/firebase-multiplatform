/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck

import com.google.android.gms.tasks.TaskCompletionSource

/** A provider that fails every request with [message]: the provider of a factory the platform does not have. */
internal fun unavailableProvider(message: String): AppCheckProvider = AppCheckProvider {
    TaskCompletionSource<AppCheckToken>().apply { setException(UnsupportedOperationException(message)) }.task
}
