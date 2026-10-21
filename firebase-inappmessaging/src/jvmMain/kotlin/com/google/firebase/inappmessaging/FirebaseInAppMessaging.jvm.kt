/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.inappmessaging

import kotlin.concurrent.Volatile

/**
 * firebase-java-sdk has no In-App Messaging: no message is ever fetched or displayed on the JVM, so the listeners and
 * the display component are never called, and the settings are only kept in memory.
 */
public actual class FirebaseInAppMessaging private constructor() {
    @Volatile
    private var messagesSuppressed = false

    /** Does nothing: no message is displayed on the JVM. */
    public actual fun addClickListener(clickListener: FirebaseInAppMessagingClickListener) {}

    /** Does nothing: no message is displayed on the JVM. */
    public actual fun addDismissListener(dismissListener: FirebaseInAppMessagingDismissListener) {}

    /** Does nothing: no message is displayed on the JVM. */
    public actual fun addDisplayErrorListener(displayErrorListener: FirebaseInAppMessagingDisplayErrorListener) {}

    /** Does nothing: no message is displayed on the JVM. */
    public actual fun addImpressionListener(impressionListener: FirebaseInAppMessagingImpressionListener) {}

    public actual fun areMessagesSuppressed(): Boolean = messagesSuppressed

    /** Kept in memory: nothing is collected on the JVM. */
    @Volatile
    public actual var isAutomaticDataCollectionEnabled: Boolean = true

    /** Kept in memory; `null` restores the default, `true`. */
    public actual fun setAutomaticDataCollectionEnabled(isAutomaticCollectionEnabled: Boolean?) {
        isAutomaticDataCollectionEnabled = isAutomaticCollectionEnabled ?: true
    }

    public actual fun removeClickListener(clickListener: FirebaseInAppMessagingClickListener) {}

    public actual fun removeDismissListener(dismissListener: FirebaseInAppMessagingDismissListener) {}

    public actual fun removeDisplayErrorListener(displayErrorListener: FirebaseInAppMessagingDisplayErrorListener) {}

    public actual fun removeImpressionListener(impressionListener: FirebaseInAppMessagingImpressionListener) {}

    /** Does nothing: no message is displayed on the JVM. */
    public actual fun setMessageDisplayComponent(messageDisplay: FirebaseInAppMessagingDisplay) {}

    public actual fun clearDisplayListener() {}

    public actual fun setMessagesSuppressed(areMessagesSuppressed: Boolean?) {
        messagesSuppressed = areMessagesSuppressed!!
    }

    /** Does nothing: there are no campaigns on the JVM. */
    public actual fun triggerEvent(eventName: String) {}

    override fun toString(): String = "FirebaseInAppMessaging"

    public actual companion object {
        private val instance = FirebaseInAppMessaging()

        @JvmStatic
        public actual fun getInstance(): FirebaseInAppMessaging = instance
    }
}
