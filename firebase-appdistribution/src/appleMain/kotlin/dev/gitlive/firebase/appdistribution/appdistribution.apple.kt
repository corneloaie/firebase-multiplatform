/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.appdistribution

import com.google.firebase.appdistribution.IosAppDistributionRelease
import swiftPMImport.dev.gitlive.firebase.appdistribution.FIRAppDistribution
import swiftPMImport.dev.gitlive.firebase.appdistribution.FIRAppDistributionRelease

/** The underlying Firebase iOS SDK object, a singleton of the default app. */
public val FirebaseAppDistribution.ios: FIRAppDistribution get() = FIRAppDistribution.appDistribution()

/**
 * The underlying Firebase iOS SDK release, with its build version string, download URL and whether that has expired;
 * null for a release that is not the iOS SDK's.
 */
public val AppDistributionRelease.ios: FIRAppDistributionRelease? get() = (this as? IosAppDistributionRelease)?.ios
