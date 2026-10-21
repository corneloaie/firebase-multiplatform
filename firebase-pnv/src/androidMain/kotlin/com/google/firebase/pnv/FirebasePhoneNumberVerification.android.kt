/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.pnv

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import dev.gitlive.firebase.pnv.stub

/*
 * Header stubs for com.google.firebase:firebase-pnv (see buildSrc utils/HeaderStubs.kt): compiled against, verified to
 * match the real classes, and deleted from the output so the real SDK binds at runtime. The SDK is written in Kotlin,
 * so unlike most Firebase classes it has the Companion objects that this module's own code calls. The Context member is
 * not in the common API; it is declared here so that the shipped extension (VerifiedPhoneNumber.android.kt) binds to it.
 */

public actual interface FirebasePhoneNumberVerification {
    public actual fun getDigitalCredentialPayload(nonce: String): Task<String>

    /** The SDK's Context member; the shipped extension of common code, which takes `Any`, binds to it. */
    public fun getVerifiedPhoneNumber(context: Context): Task<VerifiedPhoneNumberTokenResult>
    public actual fun exchangeCredentialResponseForPhoneNumber(dcApiResponse: String): Task<VerifiedPhoneNumberTokenResult>
    public actual fun getVerificationSupportInfo(simSlot: Int): Task<List<VerificationSupportResult>>
    public actual fun getVerificationSupportInfo(): Task<List<VerificationSupportResult>>
    public actual fun enableTestSession(testNumberId: String)

    public actual companion object {
        @JvmStatic
        public actual fun getInstance(firebaseApp: FirebaseApp): FirebasePhoneNumberVerification = stub()

        @JvmStatic
        public actual fun getInstance(): FirebasePhoneNumberVerification = stub()
    }
}

// The stub's super call is never run; Kotlin cannot pass the SDK's nullable cause to the Java FirebaseException.
public actual class FirebasePhoneNumberVerificationException actual constructor(errorCode: Int, message: String, cause: Throwable?) : FirebaseException(message) {
    public actual val errorCode: Int get() = stub()

    public actual companion object
}

public actual class VerifiedPhoneNumberTokenResult actual constructor(token: String) {
    public actual fun getToken(): String = stub()
    public actual fun getExpirationTimestamp(): Long = stub()
    public actual fun getIssuedAtTimestamp(): Long = stub()
    public actual fun getPhoneNumber(): String = stub()
    public actual fun getClaims(): Map<String, Any> = stub()
    public actual fun getNonce(): String = stub()

    public actual companion object
}
