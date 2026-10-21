/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.pnv

import com.google.android.gms.tasks.Task
import com.google.firebase.pnv.getVerifiedPhoneNumber
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.UnsupportedOnApple
import dev.gitlive.firebase.UnsupportedOnJs
import dev.gitlive.firebase.app
import dev.gitlive.firebase.runTest
import kotlinx.coroutines.tasks.await
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.google.firebase.pnv.FirebasePhoneNumberVerification as CompatFirebasePhoneNumberVerification

/**
 * Apple platforms, JS and Wasm have no Phone Number Verification SDK: the operations fail with
 * `UnsupportedOperationException` and `enableTestSession` does nothing, as the members document. The token's claims
 * are parsed as on Android.
 */
@OptIn(UnsupportedOnApple::class, UnsupportedOnJs::class)
class UnsupportedPlatformTest {

    @BeforeTest
    fun initialize() = initializeFirebase()

    @Test
    fun testTasksFail() = runTest {
        val pnv = CompatFirebasePhoneNumberVerification.getInstance()
        val tasks: List<Pair<String, Task<*>>> = listOf(
            "getVerificationSupportInfo" to pnv.getVerificationSupportInfo(),
            "getVerificationSupportInfo" to pnv.getVerificationSupportInfo(0),
            "getVerifiedPhoneNumber" to pnv.getVerifiedPhoneNumber(context),
            "getDigitalCredentialPayload" to pnv.getDigitalCredentialPayload("6b3a1c2e"),
            "exchangeCredentialResponseForPhoneNumber" to pnv.exchangeCredentialResponseForPhoneNumber("response"),
        )
        for ((member, task) in tasks) {
            assertTrue(task.isComplete, member)
            assertFalse(task.isSuccessful, member)
            assertFalse(task.isCanceled, member)
            val e = assertIs<UnsupportedOperationException>(task.exception, member)
            assertTrue(e.message.orEmpty().startsWith("FirebasePhoneNumberVerification.$member is not available on "), e.message)
            assertFailsWith<UnsupportedOperationException>(member) { task.await() }
        }
    }

    @Test
    fun testEnableTestSessionDoesNothing() {
        val pnv = CompatFirebasePhoneNumberVerification.getInstance()
        pnv.enableTestSession("test-number-id")
        // Android would throw TEST_SESSION_ALREADY_ENABLED here.
        pnv.enableTestSession("test-number-id")
        pnv.enableTestSession("")
    }

    @Test
    fun testInstance() {
        val pnv = CompatFirebasePhoneNumberVerification.getInstance()
        assertEquals(pnv, CompatFirebasePhoneNumberVerification.getInstance(Firebase.app.compat))
        assertEquals(pnv.hashCode(), CompatFirebasePhoneNumberVerification.getInstance().hashCode())
        assertEquals(Firebase.phoneNumberVerification, Firebase.phoneNumberVerification(Firebase.app))
        assertEquals(pnv, Firebase.phoneNumberVerification.compat)
    }

    @Test
    fun testKotlinFirstApiFails() = runTest {
        val pnv = Firebase.phoneNumberVerification
        assertFailsWith<UnsupportedOperationException> { pnv.getVerificationSupportInfo() }
        assertFailsWith<UnsupportedOperationException> { pnv.getVerificationSupportInfo(1) }
        assertFailsWith<UnsupportedOperationException> { pnv.getVerifiedPhoneNumber(context) }
        assertFailsWith<UnsupportedOperationException> { pnv.getDigitalCredentialPayload("6b3a1c2e") }
        assertFailsWith<UnsupportedOperationException> { pnv.exchangeCredentialResponseForPhoneNumber("response") }
        pnv.enableTestSession("test-number-id")
    }

    @Test
    fun testTokenClaimTypes() {
        val claims = VerifiedPhoneNumberTokenResult(
            jwt(
                """{"sub":"+15555550100","nonce":"n","big":4102444800000,"fraction":1.5,"exponent":1e3,"verified":true,""" +
                    """"none":null,"nested":{"b":2,"a":[1,"x",null]}}""",
            ),
        ).getClaims()
        assertEquals(4102444800000L, claims["big"])
        assertEquals(1.5, claims["fraction"])
        assertEquals(1000.0, claims["exponent"])
        assertEquals(true, claims["verified"])
        // org.json keeps a null as JSONObject.NULL; a Map<String, Any> cannot hold one, so it is left out.
        assertFalse("none" in claims)
        val nested = assertIs<Map<*, *>>(claims["nested"])
        assertEquals(listOf("b", "a"), nested.keys.toList())
        assertEquals(2, nested["b"])
        assertEquals(listOf(1, "x", null), nested["a"])
        assertNotNull(claims["sub"])
    }
}
