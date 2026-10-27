/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.pnv

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import io.github.corneloaie.firebase.UnsupportedOnApple

/*
 * Firebase has no Apple SDK for Phone Number Verification: every task fails with UnsupportedOperationException, and
 * enableTestSession does nothing.
 */

private const val PLATFORM = "Apple platforms"

public actual interface FirebasePhoneNumberVerification {
    /** Fails with `UnsupportedOperationException`: Phone Number Verification has no Apple SDK. */
    @UnsupportedOnApple
    public actual fun getDigitalCredentialPayload(nonce: String): Task<String>

    /** Fails with `UnsupportedOperationException`: Phone Number Verification has no Apple SDK. */
    @UnsupportedOnApple
    public actual fun exchangeCredentialResponseForPhoneNumber(dcApiResponse: String): Task<VerifiedPhoneNumberTokenResult>

    /** Fails with `UnsupportedOperationException`: Phone Number Verification has no Apple SDK. */
    @UnsupportedOnApple
    public actual fun getVerificationSupportInfo(simSlot: Int): Task<List<VerificationSupportResult>>

    /** Fails with `UnsupportedOperationException`: Phone Number Verification has no Apple SDK. */
    @UnsupportedOnApple
    public actual fun getVerificationSupportInfo(): Task<List<VerificationSupportResult>>

    /** Does nothing: Phone Number Verification has no Apple SDK. */
    @UnsupportedOnApple
    public actual fun enableTestSession(testNumberId: String)

    public actual companion object {
        /** The instance of [firebaseApp], whose tasks fail with `UnsupportedOperationException`. */
        @UnsupportedOnApple
        public actual fun getInstance(firebaseApp: FirebaseApp): FirebasePhoneNumberVerification = UnsupportedPhoneNumberVerification(firebaseApp, PLATFORM)

        /** The instance of the default app, whose tasks fail with `UnsupportedOperationException`. */
        @UnsupportedOnApple
        public actual fun getInstance(): FirebasePhoneNumberVerification = getInstance(FirebaseApp.getInstance())
    }
}

/** Fails with `UnsupportedOperationException`: Phone Number Verification has no Apple SDK. */
@UnsupportedOnApple
public actual fun FirebasePhoneNumberVerification.getVerifiedPhoneNumber(context: Any): Task<VerifiedPhoneNumberTokenResult> = unsupportedTask("getVerifiedPhoneNumber", PLATFORM)
