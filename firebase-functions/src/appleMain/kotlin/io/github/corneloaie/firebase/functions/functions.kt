/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.functions

import swiftPMImport.io.github.corneloaie.firebase.functions.FIRFunctions
import swiftPMImport.io.github.corneloaie.firebase.functions.FIRHTTPSCallable
import swiftPMImport.io.github.corneloaie.firebase.functions.FIRHTTPSCallableResult

/** The underlying Firebase iOS SDK object. */
public val FirebaseFunctions.ios: FIRFunctions get() = compat.ios

/** The underlying Firebase iOS SDK object. */
public val HttpsCallableReference.ios: FIRHTTPSCallable get() = compat.ios

/** The underlying Firebase iOS SDK object. */
public val HttpsCallableResult.ios: FIRHTTPSCallableResult get() = compat.ios
