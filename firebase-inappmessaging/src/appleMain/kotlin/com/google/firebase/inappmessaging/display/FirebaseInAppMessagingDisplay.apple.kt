/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.inappmessaging.display

import com.google.firebase.inappmessaging.CallbacksDelegate
import com.google.firebase.inappmessaging.DisplayCallbacks
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks.InAppMessagingErrorReason
import com.google.firebase.inappmessaging.model.InAppMessage
import com.google.firebase.inappmessaging.model.ios
import platform.Foundation.NSClassFromString
import platform.darwin.NSObjectMeta
import swiftPMImport.io.github.corneloaie.firebase.inappmessaging.FIRInAppMessagingDisplayProtocol

/**
 * The iOS SDK's default UI, `FIRIAMDefaultDisplayImpl`, which the iOS SDK sets as the display component when the app is
 * configured. The class is not in the iOS SDK's public headers, so it is looked up by name; it exists on iOS only.
 *
 * @property ios The iOS SDK's default display component; `null` on tvOS, which has no default UI.
 */
public actual class FirebaseInAppMessagingDisplay internal constructor(public val ios: FIRInAppMessagingDisplayProtocol?) : com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplay {

    /**
     * Renders the message with the iOS SDK's default UI, which reports to [callbacks] on the main thread. Reports
     * [InAppMessagingErrorReason.UNSPECIFIED_RENDER_ERROR] instead on tvOS, which has no default UI.
     */
    actual override fun displayMessage(inAppMessage: InAppMessage, callbacks: FirebaseInAppMessagingDisplayCallbacks) {
        val display = ios
        val message = inAppMessage.ios ?: (callbacks as? DisplayCallbacks)?.message
        if (display == null || message == null) {
            callbacks.displayErrorEncountered(InAppMessagingErrorReason.UNSPECIFIED_RENDER_ERROR)
            return
        }
        // The callbacks of a message the iOS SDK handed out wrap its own display delegate, which the default UI can take
        // directly; the default UI keeps the delegate it is given until the message is gone.
        display.displayMessage(message, (callbacks as? DisplayCallbacks)?.delegate ?: CallbacksDelegate(callbacks))
    }

    override fun toString(): String = "FirebaseInAppMessagingDisplay($ios)"

    public actual companion object {
        private val instance: FirebaseInAppMessagingDisplay by lazy {
            val defaultDisplay = (NSClassFromString("FIRIAMDefaultDisplayImpl") as? NSObjectMeta)?.new()
            FirebaseInAppMessagingDisplay(defaultDisplay as? FIRInAppMessagingDisplayProtocol)
        }

        public actual fun getInstance(): FirebaseInAppMessagingDisplay = instance
    }
}
