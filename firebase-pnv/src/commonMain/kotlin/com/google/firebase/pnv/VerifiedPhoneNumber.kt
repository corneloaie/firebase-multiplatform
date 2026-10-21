/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.pnv

import com.google.android.gms.tasks.Task

/*
 * The Android SDK takes an android.content.Context, which common code cannot name, so this takes it as Any; on Android
 * the SDK's member binds for Android code (a member wins over an extension) and this file is shipped for common code.
 */

/**
 * Verifies the phone number of a SIM in the device: asks the user for consent through the Android Credential Manager,
 * gets the number from the carrier, and returns it with its Firebase PNV token, which your backend verifies. A
 * successful verification is billed.
 *
 * [context] is an `android.content.Context` that is, or wraps, an `android.app.Activity`; otherwise the task fails with
 * a [FirebasePhoneNumberVerificationException] with [FirebasePnvStatusCodes.ACTIVITY_CONTEXT_REQUIRED]. On the platforms
 * without Phone Number Verification the task fails with `UnsupportedOperationException`.
 */
public expect fun FirebasePhoneNumberVerification.getVerifiedPhoneNumber(context: Any): Task<VerifiedPhoneNumberTokenResult>
