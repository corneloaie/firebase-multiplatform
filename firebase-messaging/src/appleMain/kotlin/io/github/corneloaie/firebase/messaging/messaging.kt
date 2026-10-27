/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.messaging

import swiftPMImport.io.github.corneloaie.firebase.messaging.FIRMessaging

/** The underlying Firebase iOS SDK object. */
public val FirebaseMessaging.ios: FIRMessaging get() = compat.ios
