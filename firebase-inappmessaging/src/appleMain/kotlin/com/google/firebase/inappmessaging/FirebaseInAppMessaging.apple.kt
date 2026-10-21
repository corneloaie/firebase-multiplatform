/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.inappmessaging

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks.InAppMessagingDismissType
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks.InAppMessagingErrorReason
import com.google.firebase.inappmessaging.model.Action
import com.google.firebase.inappmessaging.model.asAction
import com.google.firebase.inappmessaging.model.asInAppMessage
import platform.Foundation.NSError
import platform.Foundation.NSLocalizedDescriptionKey
import platform.Foundation.NSURL
import platform.Foundation.NSURLErrorDomain
import platform.Foundation.setValue
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import swiftPMImport.dev.gitlive.firebase.inappmessaging.FIAMDisplayRenderErrorType
import swiftPMImport.dev.gitlive.firebase.inappmessaging.FIRInAppMessaging
import swiftPMImport.dev.gitlive.firebase.inappmessaging.FIRInAppMessagingAction
import swiftPMImport.dev.gitlive.firebase.inappmessaging.FIRInAppMessagingDismissType
import swiftPMImport.dev.gitlive.firebase.inappmessaging.FIRInAppMessagingDisplayDelegateProtocol
import swiftPMImport.dev.gitlive.firebase.inappmessaging.FIRInAppMessagingDisplayMessage
import swiftPMImport.dev.gitlive.firebase.inappmessaging.FIRInAppMessagingDisplayProtocol
import swiftPMImport.dev.gitlive.firebase.inappmessaging.FIRInAppMessagingErrorDomain
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import com.google.firebase.inappmessaging.display.FirebaseInAppMessagingDisplay as DefaultDisplay

/**
 * The iOS SDK's `FIRInAppMessaging`. There is one instance per `FIRInAppMessaging`, as the listeners live here: they
 * are fed from its `delegate`, which this instance sets (and keeps alive, the property being weak) when a listener is
 * added, replacing a delegate set on [ios] directly.
 *
 * @property ios The underlying Firebase iOS SDK object.
 */
public actual class FirebaseInAppMessaging internal constructor(public val ios: FIRInAppMessaging) {
    private val clickListeners = Listeners<FirebaseInAppMessagingClickListener>()
    private val dismissListeners = Listeners<FirebaseInAppMessagingDismissListener>()
    private val displayErrorListeners = Listeners<FirebaseInAppMessagingDisplayErrorListener>()
    private val impressionListeners = Listeners<FirebaseInAppMessagingImpressionListener>()
    private val delegate = ListenerDelegate(clickListeners, dismissListeners, displayErrorListeners, impressionListeners)

    public actual fun addClickListener(clickListener: FirebaseInAppMessagingClickListener) {
        clickListeners.add(clickListener)
        ios.delegate = delegate
    }

    public actual fun addDismissListener(dismissListener: FirebaseInAppMessagingDismissListener) {
        dismissListeners.add(dismissListener)
        ios.delegate = delegate
    }

    public actual fun addDisplayErrorListener(displayErrorListener: FirebaseInAppMessagingDisplayErrorListener) {
        displayErrorListeners.add(displayErrorListener)
        ios.delegate = delegate
    }

    public actual fun addImpressionListener(impressionListener: FirebaseInAppMessagingImpressionListener) {
        impressionListeners.add(impressionListener)
        ios.delegate = delegate
    }

    public actual fun areMessagesSuppressed(): Boolean = ios.messageDisplaySuppressed

    public actual var isAutomaticDataCollectionEnabled: Boolean
        get() = ios.automaticDataCollectionEnabled
        set(value) {
            ios.automaticDataCollectionEnabled = value
        }

    /** `null` leaves the setting unchanged: the iOS SDK has no override to clear. */
    public actual fun setAutomaticDataCollectionEnabled(isAutomaticCollectionEnabled: Boolean?) {
        if (isAutomaticCollectionEnabled != null) ios.automaticDataCollectionEnabled = isAutomaticCollectionEnabled
    }

    public actual fun removeClickListener(clickListener: FirebaseInAppMessagingClickListener): Unit = clickListeners.remove(clickListener)

    public actual fun removeDismissListener(dismissListener: FirebaseInAppMessagingDismissListener): Unit = dismissListeners.remove(dismissListener)

    public actual fun removeDisplayErrorListener(displayErrorListener: FirebaseInAppMessagingDisplayErrorListener): Unit = displayErrorListeners.remove(displayErrorListener)

    public actual fun removeImpressionListener(impressionListener: FirebaseInAppMessagingImpressionListener): Unit = impressionListeners.remove(impressionListener)

    /**
     * Sets the iOS SDK's `messageDisplayComponent`: the iOS SDK's own default UI for
     * [com.google.firebase.inappmessaging.display.FirebaseInAppMessagingDisplay] (none on tvOS, which then displays
     * nothing), or else a component that converts each message and calls [messageDisplay] on the main thread.
     */
    public actual fun setMessageDisplayComponent(messageDisplay: FirebaseInAppMessagingDisplay) {
        val component = if (messageDisplay is DefaultDisplay) messageDisplay.ios else DisplayComponent(messageDisplay)
        if (component == null) clearDisplayListener() else ios.messageDisplayComponent = component
    }

    public actual fun clearDisplayListener() {
        // The property is declared nonnull, but nil is how the iOS SDK is told to display nothing.
        ios.setValue(null, forKey = "messageDisplayComponent")
    }

    public actual fun setMessagesSuppressed(areMessagesSuppressed: Boolean?) {
        ios.messageDisplaySuppressed = areMessagesSuppressed!!
    }

    public actual fun triggerEvent(eventName: String): Unit = ios.triggerEvent(eventName)

    override fun equals(other: Any?): Boolean = other is FirebaseInAppMessaging && other.ios == ios

    override fun hashCode(): Int = ios.hashCode()

    override fun toString(): String = "FirebaseInAppMessaging($ios)"

    public actual companion object {
        @OptIn(ExperimentalAtomicApi::class)
        private val instance = AtomicReference<FirebaseInAppMessaging?>(null)

        /** The instance wrapping `FIRInAppMessaging.inAppMessaging()`, which needs the default app to be configured. */
        @OptIn(ExperimentalAtomicApi::class)
        public actual fun getInstance(): FirebaseInAppMessaging {
            val ios = FIRInAppMessaging.inAppMessaging()
            while (true) {
                val current = instance.load()
                if (current != null && current.ios == ios) return current
                val created = FirebaseInAppMessaging(ios)
                if (instance.compareAndSet(current, created)) return created
            }
        }
    }
}

/** A set of listeners that is safe to change from any thread while it is notified. */
@OptIn(ExperimentalAtomicApi::class)
internal class Listeners<T : Any> {
    private val listeners = AtomicReference<List<T>>(emptyList())

    fun snapshot(): List<T> = listeners.load()

    fun add(listener: T) = update { if (listener in it) it else it + listener }

    fun remove(listener: T) = update { it - listener }

    private inline fun update(transform: (List<T>) -> List<T>) {
        while (true) {
            val current = listeners.load()
            if (listeners.compareAndSet(current, transform(current))) return
        }
    }
}

/** The app-side `FIRInAppMessagingDisplayDelegate`, which the iOS SDK notifies of every message event. */
private class ListenerDelegate(
    private val clickListeners: Listeners<FirebaseInAppMessagingClickListener>,
    private val dismissListeners: Listeners<FirebaseInAppMessagingDismissListener>,
    private val displayErrorListeners: Listeners<FirebaseInAppMessagingDisplayErrorListener>,
    private val impressionListeners: Listeners<FirebaseInAppMessagingImpressionListener>,
) : NSObject(),
    FIRInAppMessagingDisplayDelegateProtocol {

    override fun messageClicked(inAppMessage: FIRInAppMessagingDisplayMessage, withAction: FIRInAppMessagingAction) {
        val listeners = clickListeners.snapshot().ifEmpty { return }
        val message = inAppMessage.asInAppMessage()
        val action = withAction.asAction(message)
        listeners.forEach { it.messageClicked(message, action) }
    }

    override fun messageDismissed(inAppMessage: FIRInAppMessagingDisplayMessage, dismissType: FIRInAppMessagingDismissType) {
        val listeners = dismissListeners.snapshot().ifEmpty { return }
        val message = inAppMessage.asInAppMessage()
        listeners.forEach { it.messageDismissed(message) }
    }

    override fun impressionDetectedForMessage(inAppMessage: FIRInAppMessagingDisplayMessage) {
        val listeners = impressionListeners.snapshot().ifEmpty { return }
        val message = inAppMessage.asInAppMessage()
        listeners.forEach { it.impressionDetected(message) }
    }

    override fun displayErrorForMessage(inAppMessage: FIRInAppMessagingDisplayMessage, error: NSError) {
        val listeners = displayErrorListeners.snapshot().ifEmpty { return }
        val message = inAppMessage.asInAppMessage()
        val reason = error.asErrorReason()
        listeners.forEach { it.displayErrorEncountered(message, reason) }
    }
}

/**
 * The iOS SDK's display component for a [FirebaseInAppMessagingDisplay]: the iOS SDK calls it on a background thread,
 * the Android SDK its components on the main thread, where [display] runs here too.
 */
private class DisplayComponent(private val display: FirebaseInAppMessagingDisplay) :
    NSObject(),
    FIRInAppMessagingDisplayProtocol {

    override fun displayMessage(messageForDisplay: FIRInAppMessagingDisplayMessage, displayDelegate: FIRInAppMessagingDisplayDelegateProtocol) {
        val message = messageForDisplay.asInAppMessage()
        val callbacks = DisplayCallbacks(messageForDisplay, displayDelegate)
        dispatch_async(dispatch_get_main_queue()) { display.displayMessage(message, callbacks) }
    }
}

/**
 * The [FirebaseInAppMessagingDisplayCallbacks] of a message the iOS SDK handed to a display component: each call is
 * reported to [delegate], the iOS SDK's display delegate, on the main thread (where the iOS SDK expects it), and the task
 * completes once it has been.
 */
internal class DisplayCallbacks(
    internal val message: FIRInAppMessagingDisplayMessage,
    internal val delegate: FIRInAppMessagingDisplayDelegateProtocol,
) : FirebaseInAppMessagingDisplayCallbacks {

    override fun impressionDetected(): Task<Nothing?> = onMainThread { delegate.impressionDetectedForMessage(message) }

    override fun messageDismissed(dismissType: InAppMessagingDismissType): Task<Nothing?> = onMainThread { delegate.messageDismissed(message, dismissType.ios) }

    override fun messageClicked(action: Action): Task<Nothing?> = onMainThread { delegate.messageClicked(message, action.ios) }

    override fun displayErrorEncountered(inAppMessagingErrorReason: InAppMessagingErrorReason): Task<Nothing?> = onMainThread { delegate.displayErrorForMessage(message, inAppMessagingErrorReason.ios) }

    private fun onMainThread(report: () -> Unit): Task<Nothing?> {
        val source = TaskCompletionSource<Nothing?>()
        dispatch_async(dispatch_get_main_queue()) {
            report()
            source.setResult(null)
        }
        return source.task
    }
}

/**
 * The [FirebaseInAppMessagingDisplayCallbacks] of a message as the iOS SDK's display delegate, for the iOS SDK's default
 * UI showing a message for a component (see [com.google.firebase.inappmessaging.display.FirebaseInAppMessagingDisplay]).
 */
internal class CallbacksDelegate(private val callbacks: FirebaseInAppMessagingDisplayCallbacks) :
    NSObject(),
    FIRInAppMessagingDisplayDelegateProtocol {

    override fun messageClicked(inAppMessage: FIRInAppMessagingDisplayMessage, withAction: FIRInAppMessagingAction) {
        callbacks.messageClicked(withAction.asAction(inAppMessage.asInAppMessage()))
    }

    override fun messageDismissed(inAppMessage: FIRInAppMessagingDisplayMessage, dismissType: FIRInAppMessagingDismissType) {
        callbacks.messageDismissed(dismissType.asDismissType())
    }

    override fun impressionDetectedForMessage(inAppMessage: FIRInAppMessagingDisplayMessage) {
        callbacks.impressionDetected()
    }

    override fun displayErrorForMessage(inAppMessage: FIRInAppMessagingDisplayMessage, error: NSError) {
        callbacks.displayErrorEncountered(error.asErrorReason())
    }
}

/** The `userInfo` key under which an error reported from Kotlin keeps its [InAppMessagingErrorReason]. */
private const val ERROR_REASON_KEY = "FirebaseInAppMessagingErrorReason"

/** `FIRInAppMessagingErrorDomain`, which the header declares without nullability. */
private val errorDomain: String get() = FIRInAppMessagingErrorDomain ?: "com.firebase.inappmessaging"

private val InAppMessagingDismissType.ios: FIRInAppMessagingDismissType
    get() = when (this) {
        InAppMessagingDismissType.UNKNOWN_DISMISS_TYPE -> FIRInAppMessagingDismissType.FIRInAppMessagingDismissUnspecified
        InAppMessagingDismissType.AUTO -> FIRInAppMessagingDismissType.FIRInAppMessagingDismissTypeAuto
        InAppMessagingDismissType.CLICK -> FIRInAppMessagingDismissType.FIRInAppMessagingDismissTypeUserTapClose
        InAppMessagingDismissType.SWIPE -> FIRInAppMessagingDismissType.FIRInAppMessagingDismissTypeUserSwipe
    }

private fun FIRInAppMessagingDismissType.asDismissType(): InAppMessagingDismissType = when (this) {
    FIRInAppMessagingDismissType.FIRInAppMessagingDismissTypeAuto -> InAppMessagingDismissType.AUTO
    FIRInAppMessagingDismissType.FIRInAppMessagingDismissTypeUserTapClose -> InAppMessagingDismissType.CLICK
    FIRInAppMessagingDismissType.FIRInAppMessagingDismissTypeUserSwipe -> InAppMessagingDismissType.SWIPE
    else -> InAppMessagingDismissType.UNKNOWN_DISMISS_TYPE
}

/** The action as the iOS SDK's: its button's label as the action text. */
private val Action.ios: FIRInAppMessagingAction
    get() = FIRInAppMessagingAction(actionText = button?.text?.text, actionURL = actionUrl?.let { NSURL.URLWithString(it) })

/** The reason as an error of the iOS SDK's domain, which keeps the reason for [asErrorReason]. */
private val InAppMessagingErrorReason.ios: NSError
    get() = NSError.errorWithDomain(
        domain = errorDomain,
        code = when (this) {
            InAppMessagingErrorReason.UNSPECIFIED_RENDER_ERROR -> FIAMDisplayRenderErrorType.FIAMDisplayRenderErrorTypeUnspecifiedError
            else -> FIAMDisplayRenderErrorType.FIAMDisplayRenderErrorTypeImageDataInvalid
        }.value,
        userInfo = mapOf<Any?, Any?>(NSLocalizedDescriptionKey to "In-App Messaging display error: $name", ERROR_REASON_KEY to name),
    )

/**
 * The closest reason for an error of the iOS SDK: the image downloads fail with URL errors, and the iOS SDK's error
 * domain has the codes of `FIAMDisplayRenderErrorType`.
 */
private fun NSError.asErrorReason(): InAppMessagingErrorReason {
    (userInfo[ERROR_REASON_KEY] as? String)?.let { name -> InAppMessagingErrorReason.entries.firstOrNull { it.name == name }?.let { return it } }
    return when {
        domain == NSURLErrorDomain -> InAppMessagingErrorReason.IMAGE_FETCH_ERROR
        domain == errorDomain && code == FIAMDisplayRenderErrorType.FIAMDisplayRenderErrorTypeImageDataInvalid.value -> InAppMessagingErrorReason.IMAGE_DISPLAY_ERROR
        else -> InAppMessagingErrorReason.UNSPECIFIED_RENDER_ERROR
    }
}
