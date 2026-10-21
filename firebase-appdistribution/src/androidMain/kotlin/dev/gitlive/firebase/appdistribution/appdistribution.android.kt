/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("AppDistributionKt")
@file:JvmMultifileClass

package dev.gitlive.firebase.appdistribution

/** The underlying Firebase Android SDK object. */
public val FirebaseAppDistribution.android: com.google.firebase.appdistribution.FirebaseAppDistribution get() = compat
