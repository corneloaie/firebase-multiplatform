/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.pnv

import com.google.firebase.FirebaseException

/**
 * The exception that Phone Number Verification fails with. Its message is the one given, as on Android: the JS
 * `FirebaseException` would append the cause's message to it.
 */
public actual class FirebasePhoneNumberVerificationException actual constructor(
    public actual val errorCode: Int,
    message: String,
    cause: Throwable?,
) : FirebaseException(message, cause) {
    override val message: String? = message

    public actual companion object
}
