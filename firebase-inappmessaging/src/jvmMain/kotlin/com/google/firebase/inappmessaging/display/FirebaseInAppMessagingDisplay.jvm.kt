/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.inappmessaging.display

import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks
import com.google.firebase.inappmessaging.model.InAppMessage

/** firebase-java-sdk has no In-App Messaging, so the JVM has no message UI and nothing is ever displayed. */
public actual class FirebaseInAppMessagingDisplay private constructor() : com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplay {
    /** Does nothing: there is no message UI on the JVM. */
    actual override fun displayMessage(inAppMessage: InAppMessage, callbacks: FirebaseInAppMessagingDisplayCallbacks) {}

    override fun toString(): String = "FirebaseInAppMessagingDisplay"

    public actual companion object {
        private val instance = FirebaseInAppMessagingDisplay()

        @JvmStatic
        public actual fun getInstance(): FirebaseInAppMessagingDisplay = instance
    }
}
