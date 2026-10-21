/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.pnv

/**
 * The status codes of Phone Number Verification, the [FirebasePhoneNumberVerificationException.errorCode] of its
 * failures; mirroring `com.google.firebase.pnv.FirebasePnvStatusCodes`. On Android the object also extends Google Play
 * services' `CommonStatusCodes`, whose codes a failure can carry too.
 *
 * Plain common code: on Android it is the SDK object, whose messages [getMessage] repeats on the other platforms.
 */
public object FirebasePnvStatusCodes {
    /** The carrier of the SIM does not support Phone Number Verification. */
    public const val CARRIER_NOT_SUPPORTED: Int = 55501

    /** The Credential Manager's response, or a token, is not a valid digital credential (or JWT). */
    public const val INVALID_DIGITAL_CREDENTIAL_RESPONSE: Int = 55502

    /** The integrity check failed. */
    public const val INTEGRITY_CHECK_FAILED: Int = 55503

    /** The server could not mint the digital credentials request. */
    public const val PREFLIGHT_CHECK_FAILED: Int = 55504

    /** The API, with these parameters, is not supported at this time. */
    public const val UNSUPPORTED_OPERATION: Int = 55505

    /** The Android Credential Manager failed unexpectedly. */
    public const val CREDENTIAL_MANAGER_ERROR: Int = 55506

    /** A test number ID of [FirebasePhoneNumberVerification.enableTestSession] is empty, expired or duplicated. */
    public const val INVALID_TEST_NUMBER_ID: Int = 55507

    /** [FirebasePhoneNumberVerification.enableTestSession] was called again. */
    public const val TEST_SESSION_ALREADY_ENABLED: Int = 55508

    /** The API needs an Activity context. */
    public const val ACTIVITY_CONTEXT_REQUIRED: Int = 55509

    /** The Android SDK's description of [errorCode], one of this object's codes; any other code is an unexpected error. */
    public fun getMessage(errorCode: Int): String = when (errorCode) {
        CARRIER_NOT_SUPPORTED -> "The carrier associated with this SIM is not supported."
        INVALID_DIGITAL_CREDENTIAL_RESPONSE -> "Invalid digital credential response."
        INTEGRITY_CHECK_FAILED -> "Integrity check failed in an unexpected place."
        PREFLIGHT_CHECK_FAILED -> "Preflight check failed - we could not mint a digital credentials request on the server."
        UNSUPPORTED_OPERATION -> "The API is not supported - this API with the specified parameters is not intended to be called at this time."
        CREDENTIAL_MANAGER_ERROR -> "The Android Credential Manager has failed unexpectedly."
        INVALID_TEST_NUMBER_ID -> "Invalid test number ID(s) - might be empty, expired or duplicated."
        TEST_SESSION_ALREADY_ENABLED -> "Test session already enabled - `enableTestSession` should only be called once."
        ACTIVITY_CONTEXT_REQUIRED -> "An Activity context is required to call this API."
        else -> "An unexpected error has occurred."
    }
}
