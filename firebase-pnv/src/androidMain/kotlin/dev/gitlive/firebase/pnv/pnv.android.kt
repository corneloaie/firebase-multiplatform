/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("PhoneNumberVerificationKt")
@file:JvmMultifileClass

package dev.gitlive.firebase.pnv

/** The underlying Firebase Android SDK object. */
public val FirebasePhoneNumberVerification.android: com.google.firebase.pnv.FirebasePhoneNumberVerification get() = compat
