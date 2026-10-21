/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appdistribution

/*
 * The Android SDK takes the feedback screenshot as an android.net.Uri, which common code cannot name, so these take its
 * String form; on Android the SDK's Uri members bind for Android code (a member wins over an extension) and this file is
 * shipped for common code.
 */

/**
 * Starts an activity to collect and submit feedback from the tester, with the screenshot at [screenshotUri] (a
 * `content://` or `file://` URI of a bitmap) rather than one taken by the SDK. [additionalFormText] is the string resource
 * ID of a text shown to the tester before they submit it. Does nothing on iOS and the JVM, which have no in-app feedback.
 *
 * A null [screenshotUri] is documented by the Android SDK as no screenshot, but its full SDK (up to 16.0.0-beta21) fails
 * reading the scheme of the missing URI; to have the SDK take one, call `startFeedback(additionalFormText)`.
 */
public expect fun FirebaseAppDistribution.startFeedback(additionalFormText: Int, screenshotUri: String?)

/** As the `startFeedback` with a string resource ID and a screenshot, with the [additionalFormText] itself. */
public expect fun FirebaseAppDistribution.startFeedback(additionalFormText: CharSequence, screenshotUri: String?)
