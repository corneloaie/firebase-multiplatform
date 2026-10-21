/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.pnv

import com.google.firebase.FirebaseException

/**
 * The exception that Phone Number Verification fails with; the Android SDK's `FirebasePhoneNumberVerificationException`.
 *
 * On Android it is the SDK class. It is an expect class rather than plain common code because its `cause` may be null,
 * which the Android SDK's `FirebaseException` constructor does not accept from Kotlin.
 *
 * @param errorCode why the operation failed, see [errorCode].
 * @param message the description of the failure.
 * @param cause the exception that caused the failure. Without one, the exception gets a cause whose message is
 *   [FirebasePnvStatusCodes.getMessage] of [errorCode], as the Android SDK gives it.
 */
public expect class FirebasePhoneNumberVerificationException(errorCode: Int, message: String, cause: Throwable?) : FirebaseException {
    /**
     * Why the operation failed: one of the codes of [FirebasePnvStatusCodes], or one of the common status codes of Google
     * Play services (`com.google.android.gms.common.api.CommonStatusCodes`, such as `NETWORK_ERROR`).
     */
    public val errorCode: Int

    /** The Android SDK class's companion object, which has no public members. */
    public companion object
}
