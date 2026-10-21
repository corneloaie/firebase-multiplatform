/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.inappmessaging.display

import com.google.firebase.Firebase

/*
 * The Kotlin extension of the Android SDK's firebase-inappmessaging-display (InAppMessagingDisplayKt), as plain common
 * code: on Android the facade is a header stub that is stripped, so the SDK's own facade binds.
 */

/** The default [FirebaseInAppMessagingDisplay]; the Android SDK's `Firebase.inAppMessagingDisplay`. */
public val Firebase.inAppMessagingDisplay: FirebaseInAppMessagingDisplay
    get() = FirebaseInAppMessagingDisplay.getInstance()
