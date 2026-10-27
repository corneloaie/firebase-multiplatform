/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck

import swiftPMImport.io.github.corneloaie.firebase.appcheck.FIRAppCheckProviderFactoryProtocol

/** No reCAPTCHA factory here: a RecaptchaAppCheckProviderFactory is installed like a factory of Kotlin code. */
internal actual fun AppCheckProviderFactory.recaptchaFactoryOrNull(): FIRAppCheckProviderFactoryProtocol? = null
