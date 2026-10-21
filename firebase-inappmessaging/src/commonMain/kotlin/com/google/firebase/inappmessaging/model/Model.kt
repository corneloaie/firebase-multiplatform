/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.inappmessaging.model

/*
 * The message model of the Android SDK's com.google.firebase.inappmessaging.model. Only the SDK creates these objects
 * (their constructors and builders are hidden in the Android SDK), so they have no public constructors here either.
 *
 * On Apple platforms they are built from the iOS SDK's FIRInAppMessagingDisplayMessage classes, which describe colors
 * as UIColor and a message's text with a single color: the hex colors are written as "#rrggbb" (or "#aarrggbb" for a
 * translucent color), and the title and body of a message share the message's text color.
 */

/** The type, and so the layout, of an [InAppMessage]. */
public enum class MessageType {
    /** A type this SDK version does not know. */
    UNSUPPORTED,

    /** A [ModalMessage]. */
    MODAL,

    /** An [ImageOnlyMessage]. */
    IMAGE_ONLY,

    /** A [BannerMessage]. */
    BANNER,

    /** A [CardMessage]. */
    CARD,
}

/** A text of a message and its color. */
public expect class Text {
    /** The text, if any. */
    public val text: String?

    /** The text color, as a hex string such as `#000000`. */
    public val hexColor: String
}

/** A button of a message. */
public expect class Button {
    /** The button's label. */
    public val text: Text

    /** The button's background color, as a hex string such as `#ffffff`. */
    public val buttonHexColor: String
}

/** An action of a message: the URL it opens, and the button that triggers it, if any. */
public expect class Action {
    /** The URL the action opens, if any; without one the action dismisses the message. */
    public val actionUrl: String?

    /** The button that triggers the action; `null` when the action is a click on the message itself. */
    public val button: Button?
}

/**
 * An image of a message. The Android SDK's `getBitmapData()` is not mirrored: it returns an `android.graphics.Bitmap`,
 * and the Android SDK never sets it (the display loads [imageUrl] itself). On Apple platforms the image the iOS SDK
 * downloaded is in the `imageRawData` of the iOS message, see the `ios` extension of [InAppMessage].
 */
public expect class ImageData {
    /** The URL of the image. */
    public val imageUrl: String
}

/** The campaign a message belongs to. */
public expect class CampaignMetadata {
    /** The campaign id. */
    public val campaignId: String

    /** The campaign name, as entered in the Firebase console. */
    public val campaignName: String

    /** Whether this is a test message, sent to this device from the Firebase console. */
    public val isTestMessage: Boolean
}

/**
 * A message to display: a [BannerMessage], [CardMessage], [ImageOnlyMessage] or [ModalMessage], as its [messageType]
 * says. The deprecated members are the Android SDK's message-independent getters.
 */
public expect abstract class InAppMessage {
    /** The campaign this message belongs to. */
    public val campaignMetadata: CampaignMetadata?

    /** The custom key-value data of the campaign. */
    public val data: Map<String, String>?

    /** The type of this message. */
    public val messageType: MessageType?

    /** The action of this message; for a [CardMessage] its primary action. */
    @Deprecated("Use the member of the specific message type instead.")
    public abstract val action: Action?

    /** The button of [action], if any. */
    @Deprecated("Use the member of the specific message type instead.")
    public open val actionButton: Button?

    /** The background color of this message. */
    @Deprecated("Use the member of the specific message type instead.")
    public open val backgroundHexColor: String?

    /** The body of this message. */
    @Deprecated("Use the member of the specific message type instead.")
    public open val body: Text?

    /** The id of the campaign; [CampaignMetadata.campaignId]. */
    @Deprecated("Use campaignMetadata?.campaignId instead.")
    public open val campaignId: String?

    /** The name of the campaign; [CampaignMetadata.campaignName]. */
    @Deprecated("Use campaignMetadata?.campaignName instead.")
    public open val campaignName: String?

    /** The image of this message; for a [CardMessage] its portrait image. */
    @Deprecated("Use the member of the specific message type instead.")
    public open val imageData: ImageData?

    /**
     * The image URL of a message created by an old version of the Android SDK: always `null` for the messages the SDK
     * creates now, on Android as well. Use the image data of the specific message type.
     */
    @Deprecated("Use the member of the specific message type instead.")
    public open val imageUrl: String?

    /** Whether this is a test message; [CampaignMetadata.isTestMessage]. */
    @Deprecated("Use campaignMetadata?.isTestMessage instead.")
    public val isTestMessage: Boolean?

    /** The title of this message. */
    @Deprecated("Use the member of the specific message type instead.")
    public open val title: Text?
}

/** A banner message, shown at the top of the screen. */
public expect class BannerMessage : InAppMessage {
    /** The action of a click on the banner, if any. */
    @Suppress("OVERRIDE_DEPRECATION")
    override val action: Action?

    /** The background color. */
    @Suppress("OVERRIDE_DEPRECATION")
    override val backgroundHexColor: String

    /** The body, if any. */
    @Suppress("OVERRIDE_DEPRECATION")
    override val body: Text?

    /** The image, if any. */
    @Suppress("OVERRIDE_DEPRECATION")
    override val imageData: ImageData?

    /** The title. */
    @Suppress("OVERRIDE_DEPRECATION")
    override val title: Text
}

/** A card message, with a portrait and/or a landscape image and one or two buttons. */
public expect class CardMessage : InAppMessage {
    /** The primary action. */
    @Deprecated("Use primaryAction or secondaryAction instead.")
    override val action: Action?

    /** The background color. */
    @Suppress("OVERRIDE_DEPRECATION")
    override val backgroundHexColor: String

    /** The body, if any. */
    @Suppress("OVERRIDE_DEPRECATION")
    override val body: Text?

    /** The portrait image. */
    @Deprecated("Use portraitImageData or landscapeImageData instead.")
    override val imageData: ImageData?

    /** The image displayed in landscape orientation, if any. */
    public val landscapeImageData: ImageData?

    /** The image displayed in portrait orientation, if any. */
    public val portraitImageData: ImageData?

    /** The action of the primary button. */
    public val primaryAction: Action

    /** The action of the secondary button, if the card has one. */
    public val secondaryAction: Action?

    /** The title. */
    @Suppress("OVERRIDE_DEPRECATION")
    override val title: Text
}

/** A message that is only an image. */
public expect class ImageOnlyMessage : InAppMessage {
    /** The action of a click on the image, if any. */
    @Suppress("OVERRIDE_DEPRECATION")
    override val action: Action?

    /** The image. */
    @Suppress("OVERRIDE_DEPRECATION")
    override val imageData: ImageData
}

/** A modal message, shown in the middle of the screen. */
public expect class ModalMessage : InAppMessage {
    /** The action of the message's button, if it has one. */
    @Suppress("OVERRIDE_DEPRECATION")
    override val action: Action?

    /** The background color. */
    @Suppress("OVERRIDE_DEPRECATION")
    override val backgroundHexColor: String

    /** The body, if any. */
    @Suppress("OVERRIDE_DEPRECATION")
    override val body: Text?

    /** The image, if any. */
    @Suppress("OVERRIDE_DEPRECATION")
    override val imageData: ImageData?

    /** The title. */
    @Suppress("OVERRIDE_DEPRECATION")
    override val title: Text
}
