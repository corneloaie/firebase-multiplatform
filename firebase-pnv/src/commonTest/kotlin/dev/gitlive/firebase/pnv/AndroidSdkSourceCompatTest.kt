/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.pnv

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.pnv.FirebasePhoneNumberVerification
import com.google.firebase.pnv.FirebasePhoneNumberVerificationException
import com.google.firebase.pnv.FirebasePnvStatusCodes
import com.google.firebase.pnv.VerificationSupportResult
import com.google.firebase.pnv.VerificationSupportStatus
import com.google.firebase.pnv.VerifiedPhoneNumberTokenResult
import com.google.firebase.pnv.getVerifiedPhoneNumber
import dev.gitlive.firebase.UnsupportedOnApple
import dev.gitlive.firebase.UnsupportedOnJs
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Exercises the `com.google.firebase.pnv` layer exactly as Android app code would, on every platform. The value types
 * run for real everywhere: on Android against the SDK's own classes, so the same assertions check that the other
 * platforms behave as the SDK does. The operations need a SIM and the user's consent; they are only compiled here, in
 * [compileOnly], and the platforms without the SDK check that they fail (UnsupportedPlatformTest).
 */
@IgnoreForAndroidUnitTest
@OptIn(UnsupportedOnApple::class, UnsupportedOnJs::class)
class AndroidSdkSourceCompatTest {

    @BeforeTest
    fun initialize() = initializeFirebase()

    @Test
    fun testInstance() {
        val pnv: FirebasePhoneNumberVerification = FirebasePhoneNumberVerification.getInstance()
        assertEquals(pnv, FirebasePhoneNumberVerification.getInstance(FirebaseApp.getInstance()))
        assertEquals(pnv, FirebasePhoneNumberVerification.Companion.getInstance())
    }

    @Test
    fun testStatusCodes() {
        assertEquals(
            listOf(55501, 55502, 55503, 55504, 55505, 55506, 55507, 55508, 55509),
            listOf(
                FirebasePnvStatusCodes.CARRIER_NOT_SUPPORTED,
                FirebasePnvStatusCodes.INVALID_DIGITAL_CREDENTIAL_RESPONSE,
                FirebasePnvStatusCodes.INTEGRITY_CHECK_FAILED,
                FirebasePnvStatusCodes.PREFLIGHT_CHECK_FAILED,
                FirebasePnvStatusCodes.UNSUPPORTED_OPERATION,
                FirebasePnvStatusCodes.CREDENTIAL_MANAGER_ERROR,
                FirebasePnvStatusCodes.INVALID_TEST_NUMBER_ID,
                FirebasePnvStatusCodes.TEST_SESSION_ALREADY_ENABLED,
                FirebasePnvStatusCodes.ACTIVITY_CONTEXT_REQUIRED,
            ),
        )
        assertEquals("The carrier associated with this SIM is not supported.", FirebasePnvStatusCodes.getMessage(FirebasePnvStatusCodes.CARRIER_NOT_SUPPORTED))
        assertEquals("Invalid digital credential response.", FirebasePnvStatusCodes.getMessage(FirebasePnvStatusCodes.INVALID_DIGITAL_CREDENTIAL_RESPONSE))
        assertEquals("Integrity check failed in an unexpected place.", FirebasePnvStatusCodes.getMessage(FirebasePnvStatusCodes.INTEGRITY_CHECK_FAILED))
        assertEquals(
            "Preflight check failed - we could not mint a digital credentials request on the server.",
            FirebasePnvStatusCodes.getMessage(FirebasePnvStatusCodes.PREFLIGHT_CHECK_FAILED),
        )
        assertEquals(
            "The API is not supported - this API with the specified parameters is not intended to be called at this time.",
            FirebasePnvStatusCodes.getMessage(FirebasePnvStatusCodes.UNSUPPORTED_OPERATION),
        )
        assertEquals("The Android Credential Manager has failed unexpectedly.", FirebasePnvStatusCodes.getMessage(FirebasePnvStatusCodes.CREDENTIAL_MANAGER_ERROR))
        assertEquals(
            "Invalid test number ID(s) - might be empty, expired or duplicated.",
            FirebasePnvStatusCodes.getMessage(FirebasePnvStatusCodes.INVALID_TEST_NUMBER_ID),
        )
        assertEquals(
            "Test session already enabled - `enableTestSession` should only be called once.",
            FirebasePnvStatusCodes.getMessage(FirebasePnvStatusCodes.TEST_SESSION_ALREADY_ENABLED),
        )
        assertEquals("An Activity context is required to call this API.", FirebasePnvStatusCodes.getMessage(FirebasePnvStatusCodes.ACTIVITY_CONTEXT_REQUIRED))
        // CommonStatusCodes.NETWORK_ERROR (7), and a code nobody defines: the SDK's message for anything else.
        assertEquals("An unexpected error has occurred.", FirebasePnvStatusCodes.getMessage(7))
        assertEquals("An unexpected error has occurred.", FirebasePnvStatusCodes.getMessage(55510))
        assertSame(FirebasePnvStatusCodes, FirebasePnvStatusCodes)
    }

    @Test
    fun testVerificationSupportStatus() {
        assertEquals(
            listOf(0, 1, 2, 3, 4),
            listOf(
                VerificationSupportStatus.CAPABILITY_STATUS_UNSPECIFIED,
                VerificationSupportStatus.CAPABLE,
                VerificationSupportStatus.INCAPABLE_DUE_TO_CARRIER_UNSUPPORTED,
                VerificationSupportStatus.INCAPABLE_DUE_TO_ANDROID_VERSION,
                VerificationSupportStatus.INCAPABLE_DUE_TO_SIM_STATE,
            ),
        )
        assertEquals(VerificationSupportStatus.CAPABLE, VerificationSupportStatus.Companion.CAPABLE)
    }

    @Test
    fun testVerificationSupportResult() {
        val supported = VerificationSupportResult(simSlotIndex = 1, carrierId = "310260", reason = VerificationSupportStatus.CAPABLE)
        assertEquals(1, supported.getSimSlot())
        assertEquals("310260", supported.getCarrierId())
        assertEquals(VerificationSupportStatus.CAPABLE, supported.getReason())
        assertTrue(supported.isSupported())

        for (reason in listOf(
            VerificationSupportStatus.CAPABILITY_STATUS_UNSPECIFIED,
            VerificationSupportStatus.INCAPABLE_DUE_TO_CARRIER_UNSUPPORTED,
            VerificationSupportStatus.INCAPABLE_DUE_TO_ANDROID_VERSION,
            VerificationSupportStatus.INCAPABLE_DUE_TO_SIM_STATE,
        )) {
            assertFalse(VerificationSupportResult(1, "310260", reason).isSupported(), "reason $reason")
        }

        val same = VerificationSupportResult(1, "310260", VerificationSupportStatus.CAPABLE)
        assertEquals(supported, same)
        assertEquals(supported.hashCode(), same.hashCode())
        assertNotEquals(supported, VerificationSupportResult(0, "310260", VerificationSupportStatus.CAPABLE))
        assertNotEquals(supported, VerificationSupportResult(1, "310410", VerificationSupportStatus.CAPABLE))
        assertNotEquals(supported, VerificationSupportResult(1, "310260", VerificationSupportStatus.INCAPABLE_DUE_TO_SIM_STATE))
        assertNotEquals<Any?>(supported, "310260")
    }

    @Test
    fun testException() {
        val cause = IllegalStateException("cause")
        val e = FirebasePhoneNumberVerificationException(FirebasePnvStatusCodes.CARRIER_NOT_SUPPORTED, "Not supported", cause)
        assertIs<FirebaseException>(e)
        assertEquals(FirebasePnvStatusCodes.CARRIER_NOT_SUPPORTED, e.errorCode)
        assertEquals("Not supported", e.message)
        assertSame(cause, e.cause)
        assertNull(FirebasePhoneNumberVerificationException(errorCode = 7, message = "Network error", cause = null).cause)
    }

    @Test
    fun testTokenResult() {
        val token = jwt(
            """{"iss":"https://fpnv.googleapis.com/projects/846484016111","sub":"+15555550100","nonce":"6b3a1c2e",""" +
                """"aud":["https://fpnv.googleapis.com/projects/846484016111","https://fpnv.googleapis.com/projects/fir-kotlin-sdk"],""" +
                """"exp":1767225600,"iat":1767222000}""",
        )
        val result = VerifiedPhoneNumberTokenResult(token)
        assertEquals(token, result.getToken())
        assertEquals("+15555550100", result.getPhoneNumber())
        assertEquals("6b3a1c2e", result.getNonce())
        assertEquals(1767225600L, result.getExpirationTimestamp())
        assertEquals(1767222000L, result.getIssuedAtTimestamp())

        val claims = result.getClaims()
        // Sorted by name, as the SDK's TreeMap is.
        assertEquals(listOf("aud", "exp", "iat", "iss", "nonce", "sub"), claims.keys.toList())
        assertEquals("https://fpnv.googleapis.com/projects/846484016111", claims["iss"])
        assertEquals("+15555550100", claims["sub"])
        assertEquals(1767225600L, (claims["exp"] as Number).toLong())
        assertTrue(claims["exp"] is Int, "${claims["exp"]}")
        // A padded payload parses too.
        assertEquals("+15555550100", VerifiedPhoneNumberTokenResult(paddedJwt("""{"sub":"+15555550100"}""")).getPhoneNumber())
    }

    @Test
    fun testTokenResultWithoutTimestamps() {
        val result = VerifiedPhoneNumberTokenResult(jwt("""{"sub":"+15555550100","nonce":"6b3a1c2e","exp":"soon"}"""))
        assertEquals(0L, result.getExpirationTimestamp())
        assertEquals(0L, result.getIssuedAtTimestamp())
        assertEquals("soon", result.getClaims()["exp"])
    }

    @Test
    fun testInvalidTokens() {
        val invalid = listOf(
            "" to "Token is null or empty",
            "header.payload" to "Invalid JWT format",
            "a.b.c.d" to "Invalid JWT format",
            "header.%%%%.signature" to "Invalid JWT or parsing error",
            "header..signature" to "Invalid JWT or parsing error",
            jwt("[1, 2]") to "Invalid JWT or parsing error",
            jwt("""{"sub":""") to "Invalid JWT or parsing error",
        )
        for ((token, message) in invalid) {
            val e = assertFailsWith<FirebasePhoneNumberVerificationException>(token) { VerifiedPhoneNumberTokenResult(token) }
            assertEquals(FirebasePnvStatusCodes.INVALID_DIGITAL_CREDENTIAL_RESPONSE, e.errorCode, token)
            assertEquals(message, e.message, token)
        }
    }

    /** As [jwt], with the payload's base64url padding kept. */
    private fun paddedJwt(payload: String): String = jwt(payload).split(".").let { (header, body, signature) ->
        "$header.$body${"=".repeat((4 - body.length % 4) % 4)}.$signature"
    }

    /**
     * Not run: the operations, which need a SIM, a carrier that supports verification and the user's consent (or a
     * test session), and the platform's Credential Manager. Named arguments check the SDK's parameter names on Android.
     */
    @Suppress("unused")
    private fun compileOnly(pnv: FirebasePhoneNumberVerification, activity: Any): List<Any?> {
        pnv.enableTestSession(testNumberId = "test-number-id")
        pnv.getVerificationSupportInfo()
            .addOnSuccessListener { results -> results.any { it.isSupported() } }
            .addOnFailureListener { e -> (e as? FirebasePhoneNumberVerificationException)?.errorCode }
        val slot: Task<List<VerificationSupportResult>> = pnv.getVerificationSupportInfo(simSlot = 0)
        pnv.getVerifiedPhoneNumber(activity)
            .addOnSuccessListener { result -> result.getPhoneNumber() + result.getToken() }
            .addOnCanceledListener { }
        val exchange: Task<VerifiedPhoneNumberTokenResult> = pnv.getDigitalCredentialPayload(nonce = "nonce")
            .continueWithTask { payload -> pnv.exchangeCredentialResponseForPhoneNumber(dcApiResponse = payload.result) }
            .addOnCompleteListener { task -> task.result.getNonce() }
        val support = VerificationSupportResult(simSlotIndex = 0, carrierId = "310260", reason = VerificationSupportStatus.CAPABLE)
        return listOf(
            slot,
            exchange,
            support.getSimSlot(),
            FirebasePnvStatusCodes.getMessage(errorCode = FirebasePnvStatusCodes.UNSUPPORTED_OPERATION),
            FirebasePhoneNumberVerificationException(errorCode = 0, message = "", cause = null).errorCode,
            VerifiedPhoneNumberTokenResult(token = "").getClaims(),
            FirebasePhoneNumberVerification.getInstance(firebaseApp = FirebaseApp.getInstance()),
            FirebasePhoneNumberVerificationException.Companion,
            VerificationSupportResult.Companion,
            VerifiedPhoneNumberTokenResult.Companion,
        )
    }
}
