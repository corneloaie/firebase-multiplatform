/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("InAppMessagingKt")
@file:JvmMultifileClass

package dev.gitlive.firebase.inappmessaging

/** The underlying Firebase Android SDK object. */
public val FirebaseInAppMessaging.android: com.google.firebase.inappmessaging.FirebaseInAppMessaging get() = compat
