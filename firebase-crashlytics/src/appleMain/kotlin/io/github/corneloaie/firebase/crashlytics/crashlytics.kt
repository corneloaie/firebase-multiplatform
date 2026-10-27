/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.crashlytics

import swiftPMImport.io.github.corneloaie.firebase.crashlytics.FIRCrashlytics

/** The underlying Firebase iOS SDK object. */
public val FirebaseCrashlytics.ios: FIRCrashlytics get() = compat.ios
