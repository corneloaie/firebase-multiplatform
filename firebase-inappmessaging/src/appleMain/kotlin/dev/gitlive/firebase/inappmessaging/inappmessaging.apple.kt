/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.inappmessaging

import swiftPMImport.dev.gitlive.firebase.inappmessaging.FIRInAppMessaging

/** The underlying Firebase iOS SDK object. */
public val FirebaseInAppMessaging.ios: FIRInAppMessaging get() = compat.ios
