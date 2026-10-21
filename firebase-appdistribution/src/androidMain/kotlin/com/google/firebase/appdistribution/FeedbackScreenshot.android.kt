/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("FeedbackScreenshotKt")

package com.google.firebase.appdistribution

import android.net.Uri

// Shipped (see keepClasses in the build file): the String forms of the SDK's android.net.Uri members, for common code.

public actual fun FirebaseAppDistribution.startFeedback(additionalFormText: Int, screenshotUri: String?) {
    startFeedback(additionalFormText, screenshotUri?.let(Uri::parse))
}

public actual fun FirebaseAppDistribution.startFeedback(additionalFormText: CharSequence, screenshotUri: String?) {
    startFeedback(additionalFormText, screenshotUri?.let(Uri::parse))
}
