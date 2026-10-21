/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.inappmessaging.display

import com.google.firebase.Firebase
import com.google.firebase.inappmessaging.FirebaseInAppMessaging
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks
import com.google.firebase.inappmessaging.model.InAppMessage

/**
 * The SDK's default message UI, as the Android display SDK's
 * `com.google.firebase.inappmessaging.display.FirebaseInAppMessagingDisplay`. It is set up automatically, and renders
 * every message unless [FirebaseInAppMessaging.setMessageDisplayComponent] sets another component.
 *
 * - **Android:** the display SDK (`firebase-inappmessaging-display`), which binds its component whenever an activity
 *   resumes. Its `Application.ActivityLifecycleCallbacks` members are not mirrored.
 * - **iOS:** the iOS SDK's default UI. Pass [getInstance] to [FirebaseInAppMessaging.setMessageDisplayComponent] to
 *   bring it back after another component, or call [displayMessage] from a custom component to show a message with it.
 * - **tvOS:** the iOS SDK has no default UI there, so [displayMessage] reports an error.
 * - **JVM:** nothing is ever displayed.
 *
 * Get it with [getInstance] or [Firebase.inAppMessagingDisplay].
 */
public expect class FirebaseInAppMessagingDisplay : com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplay {
    /**
     * Displays [inAppMessage] with the default UI, reporting to [callbacks].
     *
     * - **Android:** does nothing. The display SDK renders through the component it binds itself, and this inherited
     *   member is a no-op.
     * - **iOS:** renders the message with the iOS SDK's default UI.
     * - **tvOS:** reports
     *   [UNSPECIFIED_RENDER_ERROR][FirebaseInAppMessagingDisplayCallbacks.InAppMessagingErrorReason.UNSPECIFIED_RENDER_ERROR],
     *   as there is no default UI.
     * - **JVM:** does nothing.
     */
    override fun displayMessage(inAppMessage: InAppMessage, callbacks: FirebaseInAppMessagingDisplayCallbacks)

    public companion object {
        /** The default message UI of the default app. */
        public fun getInstance(): FirebaseInAppMessagingDisplay
    }
}
