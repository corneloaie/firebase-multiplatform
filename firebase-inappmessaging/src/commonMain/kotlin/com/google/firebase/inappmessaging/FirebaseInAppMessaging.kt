/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.inappmessaging

import com.google.firebase.Firebase

/**
 * Firebase In-App Messaging, as the Android SDK's `com.google.firebase.inappmessaging.FirebaseInAppMessaging`: the
 * headless SDK that fetches the campaigns, decides when a message is due and records its impressions and clicks. A
 * message is rendered by the display component, which is the SDK's default UI unless [setMessageDisplayComponent]
 * replaces it.
 *
 * - **Android:** the Android SDK itself. The default UI is the display SDK, which this module depends on (see
 *   [com.google.firebase.inappmessaging.display.FirebaseInAppMessagingDisplay]).
 * - **Apple platforms:** the iOS SDK's `FIRInAppMessaging`. The listeners are fed from its `delegate`, which this class
 *   sets when a listener is added (replacing a delegate set on it directly), and [setMessageDisplayComponent] sets its
 *   `messageDisplayComponent`. The iOS SDK has a default UI on iOS only: on tvOS a message is displayed only by a
 *   component set here.
 * - **JVM:** firebase-java-sdk has no In-App Messaging, so no message is ever displayed and the members do nothing.
 *
 * In-App Messaging always belongs to the default [com.google.firebase.FirebaseApp]: get it with [getInstance] or
 * [Firebase.inAppMessaging].
 */
public expect class FirebaseInAppMessaging {
    /**
     * Registers [clickListener], which is notified when the user clicks an action of a message. Registering the same
     * listener again has no effect. On Android the listener runs on a background thread; on Apple platforms on the
     * thread the iOS SDK reports the click on, the main thread for its default UI.
     *
     * On Android the display SDK removes every listener when an activity pauses, so add the listeners again when an
     * activity resumes.
     */
    public fun addClickListener(clickListener: FirebaseInAppMessagingClickListener)

    /** Registers [dismissListener], which is notified when a message is dismissed; see [addClickListener]. */
    public fun addDismissListener(dismissListener: FirebaseInAppMessagingDismissListener)

    /**
     * Registers [displayErrorListener], which is notified when a message cannot be displayed; see [addClickListener].
     * On Apple platforms the iOS SDK's `NSError` is mapped onto the closest
     * [FirebaseInAppMessagingDisplayCallbacks.InAppMessagingErrorReason]: a failed image download is
     * [IMAGE_FETCH_ERROR][FirebaseInAppMessagingDisplayCallbacks.InAppMessagingErrorReason.IMAGE_FETCH_ERROR], invalid
     * image data
     * [IMAGE_DISPLAY_ERROR][FirebaseInAppMessagingDisplayCallbacks.InAppMessagingErrorReason.IMAGE_DISPLAY_ERROR], and
     * anything else
     * [UNSPECIFIED_RENDER_ERROR][FirebaseInAppMessagingDisplayCallbacks.InAppMessagingErrorReason.UNSPECIFIED_RENDER_ERROR];
     * the reason a display component set with [setMessageDisplayComponent] reports is kept as it is.
     */
    public fun addDisplayErrorListener(displayErrorListener: FirebaseInAppMessagingDisplayErrorListener)

    /**
     * Registers [impressionListener], which is notified when a message has been displayed long enough to count as an
     * impression; see [addClickListener].
     */
    public fun addImpressionListener(impressionListener: FirebaseInAppMessagingImpressionListener)

    /**
     * Whether the display of messages is suppressed, see [setMessagesSuppressed]. On Android only the display SDK honours
     * it, so a custom display component should check it; the iOS SDK displays no message while it is set.
     */
    public fun areMessagesSuppressed(): Boolean

    /**
     * Whether automatic data collection is enabled: the SDK fetches messages and reports their impressions and clicks
     * only when it is. Setting it persists across app restarts and overrides the manifest (`Info.plist` on Apple
     * platforms) and the Firebase-wide setting.
     */
    public var isAutomaticDataCollectionEnabled: Boolean

    /**
     * Like setting [isAutomaticDataCollectionEnabled], where `null` clears the override on Android, so that the
     * manifest and then the Firebase-wide setting apply again. The iOS SDK has no override to clear: on Apple platforms
     * `null` leaves the setting unchanged.
     */
    public fun setAutomaticDataCollectionEnabled(isAutomaticCollectionEnabled: Boolean?)

    /** Unregisters a listener registered with [addClickListener]. */
    public fun removeClickListener(clickListener: FirebaseInAppMessagingClickListener)

    /** Unregisters a listener registered with [addDismissListener]. */
    public fun removeDismissListener(dismissListener: FirebaseInAppMessagingDismissListener)

    /** Unregisters a listener registered with [addDisplayErrorListener]. */
    public fun removeDisplayErrorListener(displayErrorListener: FirebaseInAppMessagingDisplayErrorListener)

    /** Unregisters a listener registered with [addImpressionListener]. */
    public fun removeImpressionListener(impressionListener: FirebaseInAppMessagingImpressionListener)

    /**
     * Makes [messageDisplay] render the messages, in place of the default UI. It must report what happens to the
     * message through the [FirebaseInAppMessagingDisplayCallbacks] it is given, so that the SDK records the impression
     * and click and moves on to the next message.
     *
     * - **Android:** the display SDK binds its own component again whenever an activity resumes, so a custom
     *   component only stays in place in an app that excludes `com.google.firebase:firebase-inappmessaging-display`.
     * - **Apple platforms:** [messageDisplay] runs on the main thread (the iOS SDK calls its display component on a
     *   background thread). When the user clicks an action the iOS SDK opens its URL itself, while on Android that is
     *   up to the component.
     */
    public fun setMessageDisplayComponent(messageDisplay: FirebaseInAppMessagingDisplay)

    /**
     * Removes the display component, so that no message is displayed; the Android SDK's hidden
     * `clearDisplayListener()`. On Android the display SDK binds its component again when an activity resumes. On Apple
     * platforms this also removes the iOS SDK's default UI, which
     * [com.google.firebase.inappmessaging.display.FirebaseInAppMessagingDisplay.getInstance] brings back.
     */
    public fun clearDisplayListener()

    /**
     * Suppresses (or resumes) the display of messages, until the app restarts. The parameter is a boxed `Boolean`, as in
     * the Android SDK; `null` is not allowed and throws a [NullPointerException].
     */
    public fun setMessagesSuppressed(areMessagesSuppressed: Boolean?)

    /**
     * Triggers the campaigns whose trigger is the analytics event [eventName], as if the event had been logged, and
     * displays the first one that is due.
     */
    public fun triggerEvent(eventName: String)

    public companion object {
        /** The [FirebaseInAppMessaging] instance of the default app. */
        public fun getInstance(): FirebaseInAppMessaging
    }
}
