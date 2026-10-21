/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appdistribution

import dev.gitlive.firebase.UnsupportedOnApple

/** Does nothing: the iOS SDK has no in-app feedback. */
@UnsupportedOnApple
public actual fun FirebaseAppDistribution.startFeedback(additionalFormText: Int, screenshotUri: String?) {}

/** Does nothing: the iOS SDK has no in-app feedback. */
@UnsupportedOnApple
public actual fun FirebaseAppDistribution.startFeedback(additionalFormText: CharSequence, screenshotUri: String?) {}
