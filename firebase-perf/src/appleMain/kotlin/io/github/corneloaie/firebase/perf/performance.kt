/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.perf

import swiftPMImport.io.github.corneloaie.firebase.perf.FIRPerformance

/** The underlying Firebase iOS SDK object. */
public val FirebasePerformance.ios: FIRPerformance get() = compat.ios
