/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.pnv

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp

/**
 * Firebase Phone Number Verification, mirroring `com.google.firebase.pnv.FirebasePhoneNumberVerification` from the
 * Firebase Android SDK (`com.google.firebase:firebase-pnv`): gets the phone number of a SIM in the device directly from
 * its carrier, with a token signed by Firebase for your backend to verify, instead of sending an SMS code.
 *
 * Phone Number Verification exists on Android only: Firebase has no iOS, web or Java SDK for it.
 *
 * - **Android:** the SDK itself. [getVerifiedPhoneNumber] (the extension, for common code, or the SDK's member taking an
 *   `android.content.Context`) needs an Activity.
 * - **iOS, macOS, tvOS, JS and Wasm:** every task fails with `UnsupportedOperationException` and [enableTestSession]
 *   does nothing. Their members carry `@UnsupportedOnApple` or `@UnsupportedOnJs`, which warn where code compiled for
 *   those platforms calls them; keep an SMS sign-in (Firebase Authentication's phone sign-in) for them.
 */
public expect interface FirebasePhoneNumberVerification {
    /**
     * Starts a verification through your own Android Credential Manager request: returns the server-signed payload
     * that goes into the request's Digital Credentials query. [nonce] is a single-use value your server generated; the
     * request must carry the same one, and so does the resulting token. Pass the Credential Manager's response to
     * [exchangeCredentialResponseForPhoneNumber].
     */
    public fun getDigitalCredentialPayload(nonce: String): Task<String>

    /**
     * Exchanges [dcApiResponse], the Firebase token of the Credential Manager's response to the request built with
     * [getDigitalCredentialPayload], for the verified phone number and its Firebase PNV token. A successful exchange is
     * billed.
     */
    public fun exchangeCredentialResponseForPhoneNumber(dcApiResponse: String): Task<VerifiedPhoneNumberTokenResult>

    /**
     * Whether the SIM in [simSlot] supports verification, as the one result of a list. The slot is 0 or 1; on Android
     * another one throws `IllegalArgumentException`.
     */
    public fun getVerificationSupportInfo(simSlot: Int): Task<List<VerificationSupportResult>>

    /**
     * Whether each SIM of the device supports verification, one result per SIM (in a test session, one for the test
     * number). Asks the user for nothing; check it before offering verification, and fall back to SMS otherwise.
     */
    public fun getVerificationSupportInfo(): Task<List<VerificationSupportResult>>

    /**
     * Enables a test session with [testNumberId], a test token generated in the Firebase console (Security, Phone
     * Verification, Testing), so verification works without a SIM. Call it at most once per instance: on Android a
     * second call throws a [FirebasePhoneNumberVerificationException] with
     * [FirebasePnvStatusCodes.TEST_SESSION_ALREADY_ENABLED], and an empty ID one with
     * [FirebasePnvStatusCodes.INVALID_TEST_NUMBER_ID].
     */
    public fun enableTestSession(testNumberId: String)

    public companion object {
        /** The [FirebasePhoneNumberVerification] of [firebaseApp]. */
        public fun getInstance(firebaseApp: FirebaseApp): FirebasePhoneNumberVerification

        /**
         * The [FirebasePhoneNumberVerification] of the default [FirebaseApp]; throws `IllegalStateException` if it is not
         * initialized.
         */
        public fun getInstance(): FirebasePhoneNumberVerification
    }
}
