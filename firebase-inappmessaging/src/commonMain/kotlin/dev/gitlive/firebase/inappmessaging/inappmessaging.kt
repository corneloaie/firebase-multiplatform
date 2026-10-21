/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("InAppMessagingKt")
@file:JvmMultifileClass

package dev.gitlive.firebase.inappmessaging

import com.google.firebase.inappmessaging.FirebaseInAppMessagingClickListener
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDismissListener
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplay
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks.InAppMessagingErrorReason
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayErrorListener
import com.google.firebase.inappmessaging.FirebaseInAppMessagingImpressionListener
import com.google.firebase.inappmessaging.model.Action
import com.google.firebase.inappmessaging.model.InAppMessage
import dev.gitlive.firebase.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import com.google.firebase.Firebase as CompatFirebase
import com.google.firebase.inappmessaging.FirebaseInAppMessaging as CompatFirebaseInAppMessaging
import com.google.firebase.inappmessaging.inAppMessaging as compatInAppMessaging

// The Android-SDK-shaped entry point is reached through the `Firebase.inAppMessaging` extension (InAppMessagingKt),
// which is a real static method on every platform, rather than the companion object of the header stub.

/** Returns the [FirebaseInAppMessaging] instance of the default app, the only app In-App Messaging works with. */
public val Firebase.inAppMessaging: FirebaseInAppMessaging
    get() = FirebaseInAppMessaging(CompatFirebase.compatInAppMessaging)

/**
 * Firebase In-App Messaging: displays the messages of the campaigns created in the Firebase console, with the SDK's
 * default UI or a component set with [setMessageDisplayComponent], and reports what the user does with them as flows.
 *
 * The messages, their actions and the display callbacks are the types of the `com.google.firebase.inappmessaging`
 * layer, such as [InAppMessage] and [Action]. See [CompatFirebaseInAppMessaging] for the platform differences: on the
 * JVM no message is ever displayed.
 *
 * @property compat The Android-SDK-shaped [com.google.firebase.inappmessaging.FirebaseInAppMessaging] this wraps.
 */
public class FirebaseInAppMessaging internal constructor(public val compat: CompatFirebaseInAppMessaging) {

    /**
     * Whether automatic data collection is enabled: the SDK fetches messages and reports their impressions and clicks
     * only when it is. Setting it persists across app restarts and overrides the manifest (`Info.plist` on Apple
     * platforms) and the Firebase-wide setting.
     */
    public var isAutomaticDataCollectionEnabled: Boolean
        get() = compat.isAutomaticDataCollectionEnabled
        set(value) {
            compat.isAutomaticDataCollectionEnabled = value
        }

    /**
     * Sets [isAutomaticDataCollectionEnabled], or with `null` clears the override on Android, so that the manifest and
     * then the Firebase-wide setting apply again. The iOS SDK has no override to clear: on Apple platforms `null` leaves
     * the setting unchanged.
     */
    public fun setAutomaticDataCollectionEnabled(enabled: Boolean?) {
        compat.setAutomaticDataCollectionEnabled(enabled)
    }

    /**
     * Whether the display of messages is suppressed, until it is set to `false` again or the app restarts. On Android
     * only the display SDK honours it, so a custom display component should check it; the iOS SDK displays no message
     * while it is set.
     */
    public var messagesSuppressed: Boolean
        get() = compat.areMessagesSuppressed()
        set(value) {
            compat.setMessagesSuppressed(value)
        }

    /**
     * Triggers the campaigns whose trigger is the analytics event [eventName], as if the event had been logged, and
     * displays the first one that is due.
     */
    public fun triggerEvent(eventName: String) {
        compat.triggerEvent(eventName)
    }

    /**
     * Renders the messages with [messageDisplay] in place of the default UI, or with `null` displays no message at all.
     * The default UI is [com.google.firebase.inappmessaging.display.FirebaseInAppMessagingDisplay]; see
     * [CompatFirebaseInAppMessaging.setMessageDisplayComponent] for what a component has to do, and why on Android it
     * only stays in place without the display SDK.
     */
    public fun setMessageDisplayComponent(messageDisplay: FirebaseInAppMessagingDisplay?) {
        if (messageDisplay == null) compat.clearDisplayListener() else compat.setMessageDisplayComponent(messageDisplay)
    }

    /**
     * The clicks on the actions of messages, while collected.
     *
     * On Android the listeners behind these flows run on a background thread, and the display SDK removes every
     * listener when an activity pauses: collect them while an activity is resumed, for example with
     * `repeatOnLifecycle(Lifecycle.State.RESUMED)`, so that they are registered again when it resumes.
     */
    public val clicks: Flow<InAppMessageClick>
        get() = callbackFlow {
            val listener = FirebaseInAppMessagingClickListener { inAppMessage, action -> trySend(InAppMessageClick(inAppMessage, action)) }
            compat.addClickListener(listener)
            awaitClose { compat.removeClickListener(listener) }
        }

    /** The messages displayed long enough to count as an impression, while collected; see [clicks]. */
    public val impressions: Flow<InAppMessage>
        get() = callbackFlow {
            val listener = FirebaseInAppMessagingImpressionListener { inAppMessage -> trySend(inAppMessage) }
            compat.addImpressionListener(listener)
            awaitClose { compat.removeImpressionListener(listener) }
        }

    /** The messages dismissed, by the user or automatically, while collected; see [clicks]. */
    public val dismissals: Flow<InAppMessage>
        get() = callbackFlow {
            val listener = FirebaseInAppMessagingDismissListener { inAppMessage -> trySend(inAppMessage) }
            compat.addDismissListener(listener)
            awaitClose { compat.removeDismissListener(listener) }
        }

    /** The messages that could not be displayed, while collected; see [clicks]. */
    public val displayErrors: Flow<InAppMessageDisplayError>
        get() = callbackFlow {
            val listener = FirebaseInAppMessagingDisplayErrorListener { inAppMessage, errorReason -> trySend(InAppMessageDisplayError(inAppMessage, errorReason)) }
            compat.addDisplayErrorListener(listener)
            awaitClose { compat.removeDisplayErrorListener(listener) }
        }

    override fun equals(other: Any?): Boolean = other is FirebaseInAppMessaging && other.compat == compat

    override fun hashCode(): Int = compat.hashCode()

    override fun toString(): String = "FirebaseInAppMessaging($compat)"
}

/**
 * A click on an action of a message, emitted by [FirebaseInAppMessaging.clicks].
 *
 * @property inAppMessage The message.
 * @property action The action the user clicked.
 */
public data class InAppMessageClick(val inAppMessage: InAppMessage, val action: Action)

/**
 * A message that could not be displayed, emitted by [FirebaseInAppMessaging.displayErrors].
 *
 * @property inAppMessage The message.
 * @property errorReason Why it could not be displayed.
 */
public data class InAppMessageDisplayError(val inAppMessage: InAppMessage, val errorReason: InAppMessagingErrorReason)
