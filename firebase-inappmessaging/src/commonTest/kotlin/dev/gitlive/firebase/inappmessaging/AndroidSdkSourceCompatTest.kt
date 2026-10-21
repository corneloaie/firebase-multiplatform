/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.inappmessaging

import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.inappmessaging.FirebaseInAppMessaging
import com.google.firebase.inappmessaging.FirebaseInAppMessagingClickListener
import com.google.firebase.inappmessaging.FirebaseInAppMessagingContextualTrigger
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDismissListener
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplay
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks.InAppMessagingDismissType
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayCallbacks.InAppMessagingErrorReason
import com.google.firebase.inappmessaging.FirebaseInAppMessagingDisplayErrorListener
import com.google.firebase.inappmessaging.FirebaseInAppMessagingImpressionListener
import com.google.firebase.inappmessaging.display.inAppMessagingDisplay
import com.google.firebase.inappmessaging.inAppMessaging
import com.google.firebase.inappmessaging.model.Action
import com.google.firebase.inappmessaging.model.BannerMessage
import com.google.firebase.inappmessaging.model.Button
import com.google.firebase.inappmessaging.model.CampaignMetadata
import com.google.firebase.inappmessaging.model.CardMessage
import com.google.firebase.inappmessaging.model.ImageData
import com.google.firebase.inappmessaging.model.ImageOnlyMessage
import com.google.firebase.inappmessaging.model.InAppMessage
import com.google.firebase.inappmessaging.model.MessageType
import com.google.firebase.inappmessaging.model.ModalMessage
import com.google.firebase.inappmessaging.model.Text
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import com.google.firebase.inappmessaging.display.FirebaseInAppMessagingDisplay as DefaultDisplay

/**
 * Exercises the `com.google.firebase.inappmessaging` layer exactly as Android app code would (static accessors, the
 * Kotlin extensions, SAM-converted listeners, a custom display component reading the message model), on every
 * platform. No campaign targets the test app, so the message model is only read by the component, never called.
 */
@IgnoreForAndroidUnitTest
class AndroidSdkSourceCompatTest {

    @BeforeTest
    fun initialize() {
        initializeFirebase()
    }

    @Test
    fun testSettings() {
        val inAppMessaging: FirebaseInAppMessaging = FirebaseInAppMessaging.getInstance()
        assertSame(inAppMessaging, Firebase.inAppMessaging)

        inAppMessaging.setMessagesSuppressed(true)
        assertTrue(inAppMessaging.areMessagesSuppressed())
        inAppMessaging.setMessagesSuppressed(false)
        assertFalse(inAppMessaging.areMessagesSuppressed())

        val enabled = inAppMessaging.isAutomaticDataCollectionEnabled
        inAppMessaging.isAutomaticDataCollectionEnabled = false
        assertFalse(inAppMessaging.isAutomaticDataCollectionEnabled)
        inAppMessaging.setAutomaticDataCollectionEnabled(true)
        assertTrue(inAppMessaging.isAutomaticDataCollectionEnabled)
        inAppMessaging.setAutomaticDataCollectionEnabled(null)
        inAppMessaging.isAutomaticDataCollectionEnabled = enabled

        inAppMessaging.triggerEvent("compat_test_event")
    }

    @Test
    fun testListeners() {
        val inAppMessaging = Firebase.inAppMessaging
        val clicks = FirebaseInAppMessagingClickListener { inAppMessage, action -> describe(inAppMessage) + describe(action) }
        val dismissals = FirebaseInAppMessagingDismissListener { inAppMessage -> describe(inAppMessage) }
        val displayErrors = FirebaseInAppMessagingDisplayErrorListener { inAppMessage, errorReason -> describe(inAppMessage) + errorReason.name }
        val impressions = FirebaseInAppMessagingImpressionListener { inAppMessage -> describe(inAppMessage) }

        inAppMessaging.addClickListener(clicks)
        inAppMessaging.addDismissListener(dismissals)
        inAppMessaging.addDisplayErrorListener(displayErrors)
        inAppMessaging.addImpressionListener(impressions)
        inAppMessaging.triggerEvent("compat_test_event")
        inAppMessaging.removeClickListener(clicks)
        inAppMessaging.removeDismissListener(dismissals)
        inAppMessaging.removeDisplayErrorListener(displayErrors)
        inAppMessaging.removeImpressionListener(impressions)
    }

    @Test
    fun testDisplayComponent() {
        val inAppMessaging = Firebase.inAppMessaging
        inAppMessaging.setMessageDisplayComponent(
            FirebaseInAppMessagingDisplay { inAppMessage, callbacks ->
                describe(inAppMessage)
                if (inAppMessaging.areMessagesSuppressed()) {
                    callbacks.displayErrorEncountered(InAppMessagingErrorReason.UNSPECIFIED_RENDER_ERROR)
                } else {
                    report(inAppMessage, callbacks)
                }
            },
        )
        inAppMessaging.clearDisplayListener()

        // The default UI.
        val defaultDisplay: DefaultDisplay = DefaultDisplay.getInstance()
        assertSame(defaultDisplay, Firebase.inAppMessagingDisplay)
        inAppMessaging.setMessageDisplayComponent(defaultDisplay)
    }

    @Test
    fun testModelConstants() {
        assertEquals(listOf("UNSUPPORTED", "MODAL", "IMAGE_ONLY", "BANNER", "CARD"), MessageType.values().map { it.name })
        assertEquals(listOf("UNKNOWN_DISMISS_TYPE", "AUTO", "CLICK", "SWIPE"), InAppMessagingDismissType.values().map { it.name })
        assertEquals(
            listOf("UNSPECIFIED_RENDER_ERROR", "IMAGE_FETCH_ERROR", "IMAGE_DISPLAY_ERROR", "IMAGE_UNSUPPORTED_FORMAT"),
            InAppMessagingErrorReason.values().map { it.name },
        )
        assertEquals("compat_trigger", FirebaseInAppMessagingContextualTrigger("compat_trigger").triggerName)
    }

    /** Reports what happened to a message the way a custom display component does. */
    private fun report(inAppMessage: InAppMessage, callbacks: FirebaseInAppMessagingDisplayCallbacks): List<Task<*>> {
        val action = when (inAppMessage) {
            is CardMessage -> inAppMessage.primaryAction
            is BannerMessage -> inAppMessage.action
            is ModalMessage -> inAppMessage.action
            is ImageOnlyMessage -> inAppMessage.action
            else -> null
        }
        return listOfNotNull(
            callbacks.impressionDetected(),
            action?.let { callbacks.messageClicked(it) },
            callbacks.messageDismissed(if (action == null) InAppMessagingDismissType.AUTO else InAppMessagingDismissType.CLICK),
        )
    }

    /** Reads every member of the message model, as a display component or a listener does. */
    @Suppress("DEPRECATION")
    private fun describe(inAppMessage: InAppMessage): String = buildString {
        val metadata: CampaignMetadata? = inAppMessage.campaignMetadata
        append(metadata?.campaignId).append(metadata?.campaignName).append(metadata?.isTestMessage)
        val data: Map<String, String>? = inAppMessage.data
        append(data).append(inAppMessage.messageType)
        when (inAppMessage) {
            is BannerMessage -> {
                append(describe(inAppMessage.title)).append(inAppMessage.body?.let { describe(it) })
                append(describe(inAppMessage.imageData)).append(describe(inAppMessage.action)).append(inAppMessage.backgroundHexColor)
            }
            is CardMessage -> {
                append(describe(inAppMessage.title)).append(inAppMessage.body?.let { describe(it) }).append(inAppMessage.backgroundHexColor)
                append(describe(inAppMessage.portraitImageData)).append(describe(inAppMessage.landscapeImageData))
                append(describe(inAppMessage.primaryAction)).append(describe(inAppMessage.secondaryAction))
            }
            is ImageOnlyMessage -> append(describe(inAppMessage.imageData)).append(describe(inAppMessage.action))
            is ModalMessage -> {
                append(describe(inAppMessage.title)).append(inAppMessage.body?.let { describe(it) })
                append(describe(inAppMessage.imageData)).append(describe(inAppMessage.action)).append(inAppMessage.backgroundHexColor)
            }
        }
        // The Android SDK's deprecated, message-independent getters.
        append(inAppMessage.campaignId).append(inAppMessage.campaignName).append(inAppMessage.isTestMessage)
            .append(inAppMessage.title).append(inAppMessage.body).append(inAppMessage.imageUrl).append(inAppMessage.imageData)
            .append(inAppMessage.action).append(inAppMessage.actionButton).append(inAppMessage.backgroundHexColor)
    }

    private fun describe(action: Action?): String = "${action?.actionUrl} ${action?.button?.let { describe(it) }}"

    private fun describe(button: Button): String = "${describe(button.text)} ${button.buttonHexColor}"

    private fun describe(text: Text): String = "${text.text} ${text.hexColor}"

    private fun describe(imageData: ImageData?): String? = imageData?.imageUrl
}
