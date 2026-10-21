/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.inappmessaging

import com.google.android.gms.tasks.Task
import com.google.firebase.inappmessaging.model.Action
import com.google.firebase.inappmessaging.model.InAppMessage

/*
 * Plain common declarations: they are identical on every platform, so no expect/actual is needed. On Android they are
 * still compiled, verified against the Android SDK and stripped like the header stubs (see buildSrc
 * utils/HeaderStubs.kt), so the SDK's own interfaces bind.
 */

/** Notified when the user clicks an action of a message, mirroring `FirebaseInAppMessagingClickListener`. */
public fun interface FirebaseInAppMessagingClickListener {
    /** The user clicked [action] of [inAppMessage]. */
    public fun messageClicked(inAppMessage: InAppMessage, action: Action)
}

/** Notified when a message is dismissed, mirroring `FirebaseInAppMessagingDismissListener`. */
public fun interface FirebaseInAppMessagingDismissListener {
    /** [inAppMessage] was dismissed, by the user or automatically. */
    public fun messageDismissed(inAppMessage: InAppMessage)
}

/** Notified when a message cannot be displayed, mirroring `FirebaseInAppMessagingDisplayErrorListener`. */
public fun interface FirebaseInAppMessagingDisplayErrorListener {
    /** [inAppMessage] could not be displayed, for [errorReason]. */
    public fun displayErrorEncountered(inAppMessage: InAppMessage, errorReason: FirebaseInAppMessagingDisplayCallbacks.InAppMessagingErrorReason)
}

/** Notified of the impressions of messages, mirroring `FirebaseInAppMessagingImpressionListener`. */
public fun interface FirebaseInAppMessagingImpressionListener {
    /** [inAppMessage] was displayed long enough to count as an impression. */
    public fun impressionDetected(inAppMessage: InAppMessage)
}

/**
 * A component that renders messages, mirroring `com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplay`; set
 * it with [FirebaseInAppMessaging.setMessageDisplayComponent].
 */
public fun interface FirebaseInAppMessagingDisplay {
    /**
     * Renders [inAppMessage], reporting what happens to it through [callbacks]: the impression, a click on one of its
     * actions, its dismissal, or the error that kept it from being displayed. Until one of those is reported the SDK
     * considers the message on screen and displays no other.
     */
    public fun displayMessage(inAppMessage: InAppMessage, callbacks: FirebaseInAppMessagingDisplayCallbacks)
}

/**
 * Reports what happens to a message to the SDK, mirroring `FirebaseInAppMessagingDisplayCallbacks`: a display component
 * receives one with each message. Each call records the event and notifies the matching listeners of
 * [FirebaseInAppMessaging]; the task completes once it is recorded. On Apple platforms each call is passed to the iOS
 * SDK's display delegate on the main thread, where the iOS SDK expects it, and the task completes after that.
 */
public interface FirebaseInAppMessagingDisplayCallbacks {
    /** The message has been displayed long enough to count as an impression. */
    public fun impressionDetected(): Task<Nothing?>

    /** The message was dismissed in the way given by [dismissType]; this also counts as an impression. */
    public fun messageDismissed(dismissType: InAppMessagingDismissType): Task<Nothing?>

    /**
     * The user clicked [action] of the message; this also counts as an impression. On Apple platforms the iOS SDK then
     * opens the action's URL, if it has one.
     */
    public fun messageClicked(action: Action): Task<Nothing?>

    /** The message could not be displayed, for [inAppMessagingErrorReason]. */
    public fun displayErrorEncountered(inAppMessagingErrorReason: InAppMessagingErrorReason): Task<Nothing?>

    /** How a message was dismissed. */
    public enum class InAppMessagingDismissType {
        /** Not known. */
        UNKNOWN_DISMISS_TYPE,

        /** Automatically, after a timeout. */
        AUTO,

        /** By a click on its close button or outside it. */
        CLICK,

        /** By a swipe. */
        SWIPE,
    }

    /** Why a message could not be displayed. */
    public enum class InAppMessagingErrorReason {
        /** Another error. */
        UNSPECIFIED_RENDER_ERROR,

        /** Its image could not be downloaded. */
        IMAGE_FETCH_ERROR,

        /** Its image could not be displayed. */
        IMAGE_DISPLAY_ERROR,

        /** Its image has an unsupported format. */
        IMAGE_UNSUPPORTED_FORMAT,
    }
}

/**
 * The name of an analytics event that triggers campaigns, mirroring `FirebaseInAppMessagingContextualTrigger`.
 *
 * @property triggerName The event name.
 */
public class FirebaseInAppMessagingContextualTrigger(public val triggerName: String)
