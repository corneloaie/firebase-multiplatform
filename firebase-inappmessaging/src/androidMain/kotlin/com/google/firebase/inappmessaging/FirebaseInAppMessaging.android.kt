/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.inappmessaging

import io.github.corneloaie.firebase.inappmessaging.stub

/*
 * Header stubs for com.google.firebase:firebase-inappmessaging (see buildSrc utils/HeaderStubs.kt): compiled against,
 * verified to match the real classes, and deleted from the output so the real SDK binds at runtime.
 */

public actual class FirebaseInAppMessaging private constructor() {
    public actual fun addClickListener(clickListener: FirebaseInAppMessagingClickListener): Unit = stub()
    public actual fun addDismissListener(dismissListener: FirebaseInAppMessagingDismissListener): Unit = stub()
    public actual fun addDisplayErrorListener(displayErrorListener: FirebaseInAppMessagingDisplayErrorListener): Unit = stub()
    public actual fun addImpressionListener(impressionListener: FirebaseInAppMessagingImpressionListener): Unit = stub()
    public actual fun areMessagesSuppressed(): Boolean = stub()
    public actual var isAutomaticDataCollectionEnabled: Boolean
        get() = stub()
        set(_) = stub()
    public actual fun setAutomaticDataCollectionEnabled(isAutomaticCollectionEnabled: Boolean?): Unit = stub()
    public actual fun removeClickListener(clickListener: FirebaseInAppMessagingClickListener): Unit = stub()
    public actual fun removeDismissListener(dismissListener: FirebaseInAppMessagingDismissListener): Unit = stub()
    public actual fun removeDisplayErrorListener(displayErrorListener: FirebaseInAppMessagingDisplayErrorListener): Unit = stub()
    public actual fun removeImpressionListener(impressionListener: FirebaseInAppMessagingImpressionListener): Unit = stub()
    public actual fun setMessageDisplayComponent(messageDisplay: FirebaseInAppMessagingDisplay): Unit = stub()
    public actual fun clearDisplayListener(): Unit = stub()
    public actual fun setMessagesSuppressed(areMessagesSuppressed: Boolean?): Unit = stub()
    public actual fun triggerEvent(eventName: String): Unit = stub()

    public actual companion object {
        @JvmStatic
        public actual fun getInstance(): FirebaseInAppMessaging = stub()
    }
}
