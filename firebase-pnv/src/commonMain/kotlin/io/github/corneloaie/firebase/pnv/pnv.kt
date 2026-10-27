/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("PhoneNumberVerificationKt")
@file:JvmMultifileClass

package io.github.corneloaie.firebase.pnv

import io.github.corneloaie.firebase.Firebase
import io.github.corneloaie.firebase.FirebaseApp
import io.github.corneloaie.firebase.UnsupportedOnApple
import io.github.corneloaie.firebase.UnsupportedOnJs
import kotlinx.coroutines.tasks.await
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import com.google.firebase.pnv.FirebasePhoneNumberVerification as CompatFirebasePhoneNumberVerification
import com.google.firebase.pnv.getVerifiedPhoneNumber as compatGetVerifiedPhoneNumber

// The Android SDK is written in Kotlin: its FirebasePhoneNumberVerification has the Companion object whose getInstance
// this calls, so the header stub's companion binds to the real one on Android.

/** Returns the [FirebasePhoneNumberVerification] of the default [FirebaseApp]. */
@OptIn(UnsupportedOnApple::class, UnsupportedOnJs::class)
public val Firebase.phoneNumberVerification: FirebasePhoneNumberVerification
    get() = FirebasePhoneNumberVerification(CompatFirebasePhoneNumberVerification.getInstance())

/** Returns the [FirebasePhoneNumberVerification] of [app]. */
@OptIn(UnsupportedOnApple::class, UnsupportedOnJs::class)
public fun Firebase.phoneNumberVerification(app: FirebaseApp): FirebasePhoneNumberVerification = FirebasePhoneNumberVerification(CompatFirebasePhoneNumberVerification.getInstance(app.compat))

/**
 * Firebase Phone Number Verification: gets the phone number of a SIM in the device directly from its carrier, with a
 * token signed by Firebase for your backend to verify, instead of sending an SMS code.
 *
 * It exists on Android only. On iOS, macOS, tvOS, JS and Wasm every operation throws `UnsupportedOperationException`
 * and [enableTestSession] does nothing; keep an SMS sign-in for them. See
 * [com.google.firebase.pnv.FirebasePhoneNumberVerification] for each member's behaviour.
 *
 * @property compat The Android-SDK-shaped [com.google.firebase.pnv.FirebasePhoneNumberVerification] this wraps.
 */
@OptIn(UnsupportedOnApple::class, UnsupportedOnJs::class)
public class FirebasePhoneNumberVerification internal constructor(public val compat: CompatFirebasePhoneNumberVerification) {
    /**
     * Whether each SIM of the device supports verification, one result per SIM (in a test session, one for the test
     * number). Asks the user for nothing; check it before offering verification, and fall back to SMS otherwise.
     */
    public suspend fun getVerificationSupportInfo(): List<VerificationSupportResult> = compat.getVerificationSupportInfo().await()

    /** Whether the SIM in [simSlot] (0 or 1) supports verification, as the one result of a list. */
    public suspend fun getVerificationSupportInfo(simSlot: Int): List<VerificationSupportResult> = compat.getVerificationSupportInfo(simSlot).await()

    /**
     * Verifies the phone number of a SIM in the device: asks the user for consent through the Android Credential
     * Manager, gets the number from the carrier, and returns it with its Firebase PNV token, which your backend verifies.
     * [context] is an `android.content.Context` that is, or wraps, an `android.app.Activity`. Throws a
     * [FirebasePhoneNumberVerificationException] if the verification fails.
     */
    public suspend fun getVerifiedPhoneNumber(context: Any): VerifiedPhoneNumberTokenResult = compat.compatGetVerifiedPhoneNumber(context).await()

    /**
     * Starts a verification through your own Android Credential Manager request: returns the server-signed payload of
     * the request's Digital Credentials query, for the single-use [nonce] your server generated. Pass the Credential
     * Manager's response to [exchangeCredentialResponseForPhoneNumber].
     */
    public suspend fun getDigitalCredentialPayload(nonce: String): String = compat.getDigitalCredentialPayload(nonce).await()

    /**
     * Exchanges [dcApiResponse], the Firebase token of the Credential Manager's response to the request built with
     * [getDigitalCredentialPayload], for the verified phone number and its Firebase PNV token.
     */
    public suspend fun exchangeCredentialResponseForPhoneNumber(dcApiResponse: String): VerifiedPhoneNumberTokenResult = compat.exchangeCredentialResponseForPhoneNumber(dcApiResponse).await()

    /**
     * Enables a test session with [testNumberId], a test token generated in the Firebase console, so verification works
     * without a SIM. Call it at most once: on Android a second call throws a [FirebasePhoneNumberVerificationException].
     */
    public fun enableTestSession(testNumberId: String) {
        compat.enableTestSession(testNumberId)
    }

    override fun equals(other: Any?): Boolean = other is FirebasePhoneNumberVerification && other.compat == compat

    override fun hashCode(): Int = compat.hashCode()

    override fun toString(): String = "FirebasePhoneNumberVerification($compat)"
}

/** Whether a SIM of the device supports verification, from [FirebasePhoneNumberVerification.getVerificationSupportInfo]. */
public typealias VerificationSupportResult = com.google.firebase.pnv.VerificationSupportResult

/** The reasons of a [VerificationSupportResult], its `getReason()`. */
public typealias VerificationSupportStatus = com.google.firebase.pnv.VerificationSupportStatus

/** A verified phone number with its Firebase PNV token. */
public typealias VerifiedPhoneNumberTokenResult = com.google.firebase.pnv.VerifiedPhoneNumberTokenResult

/** The exception that a failed Phone Number Verification operation throws; its `errorCode`, one of [FirebasePnvStatusCodes], says why. */
public typealias FirebasePhoneNumberVerificationException = com.google.firebase.pnv.FirebasePhoneNumberVerificationException

/** The status codes of Phone Number Verification, the `errorCode` of a [FirebasePhoneNumberVerificationException]. */
public typealias FirebasePnvStatusCodes = com.google.firebase.pnv.FirebasePnvStatusCodes
