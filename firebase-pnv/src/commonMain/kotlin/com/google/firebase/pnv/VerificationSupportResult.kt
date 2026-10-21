/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.pnv

/**
 * Whether a SIM of the device supports Phone Number Verification, mirroring
 * `com.google.firebase.pnv.VerificationSupportResult`; from [FirebasePhoneNumberVerification.getVerificationSupportInfo].
 * Two results are equal when their SIM slot, carrier and reason are.
 *
 * Plain common code: on Android it is the SDK class, which is also `Parcelable`.
 *
 * @param simSlotIndex the slot of the SIM, see [getSimSlot].
 * @param carrierId the carrier of the SIM, see [getCarrierId].
 * @param reason one of the [VerificationSupportStatus] constants, see [getReason].
 */
public class VerificationSupportResult(private val simSlotIndex: Int, private val carrierId: String, private val reason: Int) {
    /** The slot of the SIM. */
    public fun getSimSlot(): Int = simSlotIndex

    /** The ID of the SIM's carrier. */
    public fun getCarrierId(): String = carrierId

    /** Why the SIM does or does not support verification: one of the [VerificationSupportStatus] constants. */
    public fun getReason(): Int = reason

    /** Whether the SIM supports verification: its [getReason] is [VerificationSupportStatus.CAPABLE]. */
    public fun isSupported(): Boolean = reason == VerificationSupportStatus.CAPABLE

    override fun equals(other: Any?): Boolean = this === other ||
        other is VerificationSupportResult && other.simSlotIndex == simSlotIndex && other.carrierId == carrierId && other.reason == reason

    override fun hashCode(): Int = (simSlotIndex * 31 + carrierId.hashCode()) * 31 + reason

    /** The Android SDK class's companion object, which holds its `Parcelable` `CREATOR` there; no members here. */
    public companion object
}
