/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.inappmessaging.display

import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks
import com.google.firebase.inappmessaging.model.InAppMessage
import io.github.corneloaie.firebase.inappmessaging.stub

/*
 * Header stub for com.google.firebase:firebase-inappmessaging-display (see buildSrc utils/HeaderStubs.kt). The real
 * class inherits displayMessage from its internal FirebaseInAppMessagingDisplayImpl, and also implements
 * Application.ActivityLifecycleCallbacks, which is not mirrored.
 */

public actual class FirebaseInAppMessagingDisplay private constructor() : com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplay {
    actual override fun displayMessage(inAppMessage: InAppMessage, callbacks: FirebaseInAppMessagingDisplayCallbacks): Unit = stub()

    public actual companion object {
        @JvmStatic
        public actual fun getInstance(): FirebaseInAppMessagingDisplay = stub()
    }
}
