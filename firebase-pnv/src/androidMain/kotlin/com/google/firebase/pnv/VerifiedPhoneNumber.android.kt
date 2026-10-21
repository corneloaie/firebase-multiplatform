/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("VerifiedPhoneNumberKt")

package com.google.firebase.pnv

import android.content.Context
import com.google.android.gms.tasks.Task

// Shipped (see keepClasses in the build file): the Any form of the SDK's android.content.Context member, for common code.

public actual fun FirebasePhoneNumberVerification.getVerifiedPhoneNumber(context: Any): Task<VerifiedPhoneNumberTokenResult> = getVerifiedPhoneNumber(context as Context)
