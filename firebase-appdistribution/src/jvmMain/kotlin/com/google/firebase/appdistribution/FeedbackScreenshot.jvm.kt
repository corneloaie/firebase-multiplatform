/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("FeedbackScreenshotKt")

package com.google.firebase.appdistribution

/** Does nothing: there is no in-app feedback on the JVM. */
public actual fun FirebaseAppDistribution.startFeedback(additionalFormText: Int, screenshotUri: String?) {}

/** Does nothing: there is no in-app feedback on the JVM. */
public actual fun FirebaseAppDistribution.startFeedback(additionalFormText: CharSequence, screenshotUri: String?) {}
