/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.inappmessaging.model

import dev.gitlive.firebase.inappmessaging.stub

/*
 * Header stubs for the message model of com.google.firebase:firebase-inappmessaging (see buildSrc utils/HeaderStubs.kt):
 * compiled against, verified to match the real classes, and deleted from the output so the real SDK binds at runtime.
 * The SDK's hidden constructors and builders are not mirrored, except the InAppMessage constructor its subclasses call.
 */

public actual class Text private constructor() {
    public actual val text: String? get() = stub()
    public actual val hexColor: String get() = stub()
}

public actual class Button private constructor() {
    public actual val text: Text get() = stub()
    public actual val buttonHexColor: String get() = stub()
}

public actual class Action private constructor() {
    public actual val actionUrl: String? get() = stub()
    public actual val button: Button? get() = stub()
}

public actual class ImageData private constructor() {
    public actual val imageUrl: String get() = stub()
}

public actual class CampaignMetadata private constructor() {
    public actual val campaignId: String get() = stub()
    public actual val campaignName: String get() = stub()

    @get:JvmName("getIsTestMessage")
    public actual val isTestMessage: Boolean get() = stub()
}

@Suppress("UNUSED_PARAMETER")
public actual abstract class InAppMessage internal constructor(campaignMetadata: CampaignMetadata?, messageType: MessageType?, data: Map<String, String>?) {
    public actual val campaignMetadata: CampaignMetadata? get() = stub()
    public actual val data: Map<String, String>? get() = stub()
    public actual val messageType: MessageType? get() = stub()

    @Deprecated("Use the member of the specific message type instead.")
    public actual abstract val action: Action?

    @Deprecated("Use the member of the specific message type instead.")
    public actual open val actionButton: Button? get() = stub()

    @Deprecated("Use the member of the specific message type instead.")
    public actual open val backgroundHexColor: String? get() = stub()

    @Deprecated("Use the member of the specific message type instead.")
    public actual open val body: Text? get() = stub()

    @Deprecated("Use campaignMetadata?.campaignId instead.")
    public actual open val campaignId: String? get() = stub()

    @Deprecated("Use campaignMetadata?.campaignName instead.")
    public actual open val campaignName: String? get() = stub()

    @Deprecated("Use the member of the specific message type instead.")
    public actual open val imageData: ImageData? get() = stub()

    @Deprecated("Use the member of the specific message type instead.")
    public actual open val imageUrl: String? get() = stub()

    @Deprecated("Use campaignMetadata?.isTestMessage instead.")
    @get:JvmName("getIsTestMessage")
    public actual val isTestMessage: Boolean? get() = stub()

    @Deprecated("Use the member of the specific message type instead.")
    public actual open val title: Text? get() = stub()
}

public actual class BannerMessage private constructor() : InAppMessage(null, null, null) {
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val action: Action? get() = stub()

    @Suppress("OVERRIDE_DEPRECATION")
    actual override val backgroundHexColor: String get() = stub()

    @Suppress("OVERRIDE_DEPRECATION")
    actual override val body: Text? get() = stub()

    @Suppress("OVERRIDE_DEPRECATION")
    actual override val imageData: ImageData? get() = stub()

    @Suppress("OVERRIDE_DEPRECATION")
    actual override val title: Text get() = stub()
}

public actual class CardMessage private constructor() : InAppMessage(null, null, null) {
    @Deprecated("Use primaryAction or secondaryAction instead.")
    actual override val action: Action? get() = stub()

    @Suppress("OVERRIDE_DEPRECATION")
    actual override val backgroundHexColor: String get() = stub()

    @Suppress("OVERRIDE_DEPRECATION")
    actual override val body: Text? get() = stub()

    @Deprecated("Use portraitImageData or landscapeImageData instead.")
    actual override val imageData: ImageData? get() = stub()

    public actual val landscapeImageData: ImageData? get() = stub()
    public actual val portraitImageData: ImageData? get() = stub()
    public actual val primaryAction: Action get() = stub()
    public actual val secondaryAction: Action? get() = stub()

    @Suppress("OVERRIDE_DEPRECATION")
    actual override val title: Text get() = stub()
}

public actual class ImageOnlyMessage private constructor() : InAppMessage(null, null, null) {
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val action: Action? get() = stub()

    @Suppress("OVERRIDE_DEPRECATION")
    actual override val imageData: ImageData get() = stub()
}

public actual class ModalMessage private constructor() : InAppMessage(null, null, null) {
    @Suppress("OVERRIDE_DEPRECATION")
    actual override val action: Action? get() = stub()

    @Suppress("OVERRIDE_DEPRECATION")
    actual override val backgroundHexColor: String get() = stub()

    @Suppress("OVERRIDE_DEPRECATION")
    actual override val body: Text? get() = stub()

    @Suppress("OVERRIDE_DEPRECATION")
    actual override val imageData: ImageData? get() = stub()

    @Suppress("OVERRIDE_DEPRECATION")
    actual override val title: Text get() = stub()
}
