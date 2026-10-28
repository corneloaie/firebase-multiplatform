/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.inappmessaging.model

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreGraphics.CGFloatVar
import platform.UIKit.UIColor
import swiftPMImport.io.github.corneloaie.firebase.inappmessaging.FIRInAppMessagingAction
import swiftPMImport.io.github.corneloaie.firebase.inappmessaging.FIRInAppMessagingActionButton
import swiftPMImport.io.github.corneloaie.firebase.inappmessaging.FIRInAppMessagingBannerDisplay
import swiftPMImport.io.github.corneloaie.firebase.inappmessaging.FIRInAppMessagingCardDisplay
import swiftPMImport.io.github.corneloaie.firebase.inappmessaging.FIRInAppMessagingDisplayMessage
import swiftPMImport.io.github.corneloaie.firebase.inappmessaging.FIRInAppMessagingImageData
import swiftPMImport.io.github.corneloaie.firebase.inappmessaging.FIRInAppMessagingImageOnlyDisplay
import swiftPMImport.io.github.corneloaie.firebase.inappmessaging.FIRInAppMessagingModalDisplay
import kotlin.math.roundToInt

/**
 * The iOS SDK's message this one was built from, a `FIRInAppMessagingCardDisplay`, `FIRInAppMessagingModalDisplay`,
 * `FIRInAppMessagingBannerDisplay` or `FIRInAppMessagingImageOnlyDisplay`. It has what the Android model lacks, such as
 * the trigger type and the downloaded image data (`imageRawData`).
 */
public val InAppMessage.ios: FIRInAppMessagingDisplayMessage?
    get() = native as? FIRInAppMessagingDisplayMessage

/** A message of a type this SDK version does not know, as the Android SDK creates for one. */
internal class UnsupportedMessage(campaignMetadata: CampaignMetadata?, data: Map<String, String>?, native: Any?) : InAppMessage(campaignMetadata, MessageType.UNSUPPORTED, data, native) {
    @Deprecated("Use the member of the specific message type instead.")
    override val action: Action? get() = null
}

/**
 * The message as the Android SDK models it. The iOS SDK gives a message one text color for its title and body, which
 * both [Text]s get; a modal message has an action when it has a button or an action URL, as on Android.
 */
internal fun FIRInAppMessagingDisplayMessage.asInAppMessage(): InAppMessage {
    val metadata = CampaignMetadata(campaignInfo.messageID, campaignInfo.campaignName, campaignInfo.renderAsTestMessage)
    val data = appData?.entries?.associate { (key, value) -> key.toString() to value.toString() }
    return when (this) {
        is FIRInAppMessagingCardDisplay -> {
            val textColor = textColor.hexString()
            CardMessage(
                campaignMetadata = metadata,
                title = Text(title, textColor),
                body = body?.let { Text(it, textColor) },
                portraitImageData = portraitImageData.asImageData(),
                landscapeImageData = landscapeImageData?.asImageData(),
                backgroundHexColor = displayBackgroundColor.hexString(),
                primaryAction = Action(primaryActionURL?.absoluteString, primaryActionButton.asButton()),
                secondaryAction = secondaryActionButton?.let { Action(secondaryActionURL?.absoluteString, it.asButton()) },
                data = data,
                native = this,
            )
        }

        is FIRInAppMessagingModalDisplay -> {
            val textColor = textColor.hexString()
            ModalMessage(
                campaignMetadata = metadata,
                title = Text(title, textColor),
                body = bodyText?.let { Text(it, textColor) },
                imageData = imageData?.asImageData(),
                action = if (actionButton == null && actionURL == null) null else Action(actionURL?.absoluteString, actionButton?.asButton()),
                backgroundHexColor = displayBackgroundColor.hexString(),
                data = data,
                native = this,
            )
        }

        is FIRInAppMessagingBannerDisplay -> {
            val textColor = textColor.hexString()
            BannerMessage(
                campaignMetadata = metadata,
                title = Text(title, textColor),
                body = bodyText?.let { Text(it, textColor) },
                imageData = imageData?.asImageData(),
                action = actionURL?.let { Action(it.absoluteString, null) },
                backgroundHexColor = displayBackgroundColor.hexString(),
                data = data,
                native = this,
            )
        }

        is FIRInAppMessagingImageOnlyDisplay -> ImageOnlyMessage(
            campaignMetadata = metadata,
            imageData = imageData.asImageData(),
            action = actionURL?.let { Action(it.absoluteString, null) },
            data = data,
            native = this,
        )

        else -> UnsupportedMessage(metadata, data, this)
    }
}

/**
 * The action as the Android SDK models it: the iOS SDK reports a click with the button's label and URL only, so the
 * button, with its colors, is the one of [message] that has them; without a button if none of the message's matches.
 */
internal fun FIRInAppMessagingAction.asAction(message: InAppMessage): Action {
    val url = actionURL?.absoluteString
    val actions = when (message) {
        is CardMessage -> listOfNotNull(message.primaryAction, message.secondaryAction)
        is ModalMessage -> listOfNotNull(message.action)
        is BannerMessage -> listOfNotNull(message.action)
        is ImageOnlyMessage -> listOfNotNull(message.action)
        else -> emptyList()
    }
    return actions.firstOrNull { it.actionUrl == url && it.button?.text?.text == actionText }
        ?: actions.firstOrNull { it.actionUrl == url && actionText == null }
        ?: Action(url, null)
}

private fun FIRInAppMessagingImageData.asImageData(): ImageData = ImageData(imageURL)

private fun FIRInAppMessagingActionButton.asButton(): Button = Button(Text(buttonText, buttonTextColor.hexString()), buttonBackgroundColor.hexString())

/**
 * The color as the hex string the Firebase console sets, which the iOS SDK parsed: `#rrggbb`, or `#aarrggbb` for a
 * translucent color.
 */
private fun objcnames.classes.UIColor.hexString(): String = memScoped {
    val color = this@hexString as UIColor
    val red = alloc<CGFloatVar>()
    val green = alloc<CGFloatVar>()
    val blue = alloc<CGFloatVar>()
    val alpha = alloc<CGFloatVar>()
    if (!color.getRed(red.ptr, green.ptr, blue.ptr, alpha.ptr)) return "#000000"
    fun component(value: Double): Int = (value.coerceIn(0.0, 1.0) * 255).roundToInt()
    val rgb = (component(red.value) shl 16) or (component(green.value) shl 8) or component(blue.value)
    val opacity = component(alpha.value)
    if (opacity == 255) {
        "#" + rgb.toString(16).padStart(6, '0')
    } else {
        "#" + opacity.toString(16).padStart(2, '0') + rgb.toString(16).padStart(6, '0')
    }
}
