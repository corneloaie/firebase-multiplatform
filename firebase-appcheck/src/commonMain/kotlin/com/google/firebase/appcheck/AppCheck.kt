/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

// The facade name of the Android SDK's Kotlin extensions: on Android this stub is stripped, and the shipped
// Kotlin-first code that calls these functions binds to the SDK's FirebaseAppCheckKt.
@file:JvmName("FirebaseAppCheckKt")

package com.google.firebase.appcheck

import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import kotlin.jvm.JvmName

/*
 * The Kotlin extensions of the Android SDK's firebase-appcheck, as plain common code.
 */

/** Returns the [FirebaseAppCheck] of the default [FirebaseApp]. */
public val Firebase.appCheck: FirebaseAppCheck
    get() = FirebaseAppCheck.getInstance()

/** Returns the [FirebaseAppCheck] of [app]. */
public fun Firebase.appCheck(app: FirebaseApp): FirebaseAppCheck = FirebaseAppCheck.getInstance(app)

/** Destructures an [AppCheckToken]: its [AppCheckToken.token]. */
public operator fun AppCheckToken.component1(): String = getToken()

/** Destructures an [AppCheckToken]: its [AppCheckToken.expireTimeMillis]. */
public operator fun AppCheckToken.component2(): Long = getExpireTimeMillis()
