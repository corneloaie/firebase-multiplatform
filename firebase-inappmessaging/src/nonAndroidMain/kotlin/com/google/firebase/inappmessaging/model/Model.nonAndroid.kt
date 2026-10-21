/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.inappmessaging.model

import kotlin.jvm.JvmName

/*
 * The message model off Android, as plain values with the Android SDK's equality: the messages, texts, buttons, actions
 * and images compare by content (the messages without their campaign), the campaign metadata by identity. On Apple
 * platforms they are built from the iOS SDK's messages (appleMain); firebase-java-sdk has no In-App Messaging, so the
 * JVM never creates one.
 */

public actual class Text internal constructor(
    public actual val text: String?,
    public actual val hexColor: String,
) {
    override fun equals(other: Any?): Boolean = other is Text && other.text == text && other.hexColor == hexColor

    override fun hashCode(): Int = (text?.hashCode() ?: 0) + hexColor.hashCode()

    override fun toString(): String = "Text(text=$text, hexColor=$hexColor)"
}

public actual class Button internal constructor(
    public actual val text: Text,
    public actual val buttonHexColor: String,
) {
    override fun equals(other: Any?): Boolean = other is Button && other.text == text && other.buttonHexColor == buttonHexColor

    override fun hashCode(): Int = text.hashCode() + buttonHexColor.hashCode()

    override fun toString(): String = "Button(text=$text, buttonHexColor=$buttonHexColor)"
}

public actual class Action internal constructor(
    public actual val actionUrl: String?,
    public actual val button: Button?,
) {
    override fun equals(other: Any?): Boolean = other is Action && other.actionUrl == actionUrl && other.button == button

    override fun hashCode(): Int = (actionUrl?.hashCode() ?: 0) + (button?.hashCode() ?: 0)

    override fun toString(): String = "Action(actionUrl=$actionUrl, button=$button)"
}

public actual class ImageData internal constructor(public actual val imageUrl: String) {
    override fun equals(other: Any?): Boolean = other is ImageData && other.imageUrl == imageUrl

    override fun hashCode(): Int = imageUrl.hashCode()

    override fun toString(): String = "ImageData(imageUrl=$imageUrl)"
}

public actual class CampaignMetadata internal constructor(
    public actual val campaignId: String,
    public actual val campaignName: String,
    @get:JvmName("getIsTestMessage")
    public actual val isTestMessage: Boolean,
) {
    override fun toString(): String = "CampaignMetadata(campaignId=$campaignId, campaignName=$campaignName, isTestMessage=$isTestMessage)"
}

/**
 * @param native The platform SDK's message this one was built from: the iOS SDK's `FIRInAppMessagingDisplayMessage` on
 *   Apple platforms, which the display callbacks and the default UI hand back to the iOS SDK.
 */
public actual abstract class InAppMessage internal constructor(
    public actual val campaignMetadata: CampaignMetadata?,
    public actual val messageType: MessageType?,
    public actual val data: Map<String, String>?,
    internal val native: Any?,
) {
    @Deprecated("Use the member of the specific message type instead.")
    public actual abstract val action: Action?

    @Deprecated("Use the member of the specific message type instead.")
    @Suppress("DEPRECATION")
    public actual open val actionButton: Button? get() = action?.button

    // The Android SDK's base getters return the fields of its deprecated constructor, which it no longer uses: null,
    // unless the message type overrides them.

    @Deprecated("Use the member of the specific message type instead.")
    public actual open val backgroundHexColor: String? get() = null

    @Deprecated("Use the member of the specific message type instead.")
    public actual open val body: Text? get() = null

    @Deprecated("Use campaignMetadata?.campaignId instead.")
    public actual open val campaignId: String? get() = campaignMetadata?.campaignId

    @Deprecated("Use campaignMetadata?.campaignName instead.")
    public actual open val campaignName: String? get() = campaignMetadata?.campaignName

    @Deprecated("Use the member of the specific message type instead.")
    public actual open val imageData: ImageData? get() = null

    @Deprecated("Use the member of the specific message type instead.")
    public actual open val imageUrl: String? get() = null

    @Deprecated("Use campaignMetadata?.isTestMessage instead.")
    @get:JvmName("getIsTestMessage")
    public actual val isTestMessage: Boolean? get() = campaignMetadata?.isTestMessage

    @Deprecated("Use the member of the specific message type instead.")
    public actual open val title: Text? get() = null
}

public actual class BannerMessage internal constructor(
    campaignMetadata: CampaignMetadata?,
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val title: Text,
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val body: Text?,
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val imageData: ImageData?,
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val action: Action?,
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val backgroundHexColor: String,
    data: Map<String, String>?,
    native: Any?,
) : InAppMessage(campaignMetadata, MessageType.BANNER, data, native) {
    override fun equals(other: Any?): Boolean = other is BannerMessage && other.title == title && other.body == body &&
        other.imageData == imageData && other.action == action && other.backgroundHexColor == backgroundHexColor

    override fun hashCode(): Int = title.hashCode() + (body?.hashCode() ?: 0) + (imageData?.hashCode() ?: 0) +
        (action?.hashCode() ?: 0) + backgroundHexColor.hashCode()

    override fun toString(): String = "BannerMessage(campaignMetadata=$campaignMetadata, title=$title, body=$body, imageData=$imageData, " +
        "action=$action, backgroundHexColor=$backgroundHexColor, data=$data)"
}

public actual class CardMessage internal constructor(
    campaignMetadata: CampaignMetadata?,
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val title: Text,
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val body: Text?,
    public actual val portraitImageData: ImageData?,
    public actual val landscapeImageData: ImageData?,
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val backgroundHexColor: String,
    public actual val primaryAction: Action,
    public actual val secondaryAction: Action?,
    data: Map<String, String>?,
    native: Any?,
) : InAppMessage(campaignMetadata, MessageType.CARD, data, native) {
    @Deprecated("Use primaryAction or secondaryAction instead.")
    actual override val action: Action? get() = primaryAction

    @Deprecated("Use portraitImageData or landscapeImageData instead.")
    actual override val imageData: ImageData? get() = portraitImageData

    override fun equals(other: Any?): Boolean = other is CardMessage && other.title == title && other.body == body &&
        other.portraitImageData == portraitImageData && other.landscapeImageData == landscapeImageData &&
        other.backgroundHexColor == backgroundHexColor && other.primaryAction == primaryAction && other.secondaryAction == secondaryAction

    override fun hashCode(): Int = title.hashCode() + (body?.hashCode() ?: 0) + backgroundHexColor.hashCode() + primaryAction.hashCode() +
        (secondaryAction?.hashCode() ?: 0) + (portraitImageData?.hashCode() ?: 0) + (landscapeImageData?.hashCode() ?: 0)

    override fun toString(): String = "CardMessage(campaignMetadata=$campaignMetadata, title=$title, body=$body, " +
        "portraitImageData=$portraitImageData, landscapeImageData=$landscapeImageData, backgroundHexColor=$backgroundHexColor, " +
        "primaryAction=$primaryAction, secondaryAction=$secondaryAction, data=$data)"
}

public actual class ImageOnlyMessage internal constructor(
    campaignMetadata: CampaignMetadata?,
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val imageData: ImageData,
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val action: Action?,
    data: Map<String, String>?,
    native: Any?,
) : InAppMessage(campaignMetadata, MessageType.IMAGE_ONLY, data, native) {
    override fun equals(other: Any?): Boolean = other is ImageOnlyMessage && other.imageData == imageData && other.action == action

    override fun hashCode(): Int = imageData.hashCode() + (action?.hashCode() ?: 0)

    override fun toString(): String = "ImageOnlyMessage(campaignMetadata=$campaignMetadata, imageData=$imageData, action=$action, data=$data)"
}

public actual class ModalMessage internal constructor(
    campaignMetadata: CampaignMetadata?,
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val title: Text,
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val body: Text?,
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val imageData: ImageData?,
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val action: Action?,
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val backgroundHexColor: String,
    data: Map<String, String>?,
    native: Any?,
) : InAppMessage(campaignMetadata, MessageType.MODAL, data, native) {
    override fun equals(other: Any?): Boolean = other is ModalMessage && other.title == title && other.body == body &&
        other.imageData == imageData && other.action == action && other.backgroundHexColor == backgroundHexColor

    override fun hashCode(): Int = title.hashCode() + (body?.hashCode() ?: 0) + (imageData?.hashCode() ?: 0) +
        (action?.hashCode() ?: 0) + backgroundHexColor.hashCode()

    override fun toString(): String = "ModalMessage(campaignMetadata=$campaignMetadata, title=$title, body=$body, imageData=$imageData, " +
        "action=$action, backgroundHexColor=$backgroundHexColor, data=$data)"
}
