/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.inappmessaging

import com.google.firebase.Firebase

/*
 * The Kotlin extension of the Android SDK's firebase-inappmessaging (InAppMessagingKt), as plain common code: on
 * Android the facade is a header stub that is stripped, so the SDK's own facade binds.
 */

/** The [FirebaseInAppMessaging] instance of the default app; the Android SDK's `Firebase.inAppMessaging`. */
public val Firebase.inAppMessaging: FirebaseInAppMessaging
    get() = FirebaseInAppMessaging.getInstance()
