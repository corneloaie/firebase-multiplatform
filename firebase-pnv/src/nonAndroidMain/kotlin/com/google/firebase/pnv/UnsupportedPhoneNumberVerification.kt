/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.pnv

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.FirebaseApp
import io.github.corneloaie.firebase.UnsupportedOnApple
import io.github.corneloaie.firebase.UnsupportedOnJs

/**
 * The [FirebasePhoneNumberVerification] of [app] on a platform without Phone Number Verification, which [platform] names
 * in the failures: every task fails with `UnsupportedOperationException`, and [enableTestSession] does nothing.
 */
@OptIn(UnsupportedOnApple::class, UnsupportedOnJs::class)
internal class UnsupportedPhoneNumberVerification(private val app: FirebaseApp, private val platform: String) : FirebasePhoneNumberVerification {
    override fun getDigitalCredentialPayload(nonce: String): Task<String> = unsupportedTask("getDigitalCredentialPayload", platform)

    override fun exchangeCredentialResponseForPhoneNumber(dcApiResponse: String): Task<VerifiedPhoneNumberTokenResult> = unsupportedTask("exchangeCredentialResponseForPhoneNumber", platform)

    override fun getVerificationSupportInfo(simSlot: Int): Task<List<VerificationSupportResult>> = unsupportedTask("getVerificationSupportInfo", platform)

    override fun getVerificationSupportInfo(): Task<List<VerificationSupportResult>> = unsupportedTask("getVerificationSupportInfo", platform)

    override fun enableTestSession(testNumberId: String) {}

    override fun equals(other: Any?): Boolean = other is UnsupportedPhoneNumberVerification && other.app == app

    override fun hashCode(): Int = app.hashCode()

    override fun toString(): String = "FirebasePhoneNumberVerification(unavailable on $platform)"
}

/** A task that fails with `UnsupportedOperationException`: [member] does not exist on [platform]. */
internal fun <T> unsupportedTask(member: String, platform: String): Task<T> = TaskCompletionSource<T>().apply {
    setException(UnsupportedOperationException("FirebasePhoneNumberVerification.$member is not available on $platform: Firebase Phone Number Verification is an Android SDK only"))
}.task
