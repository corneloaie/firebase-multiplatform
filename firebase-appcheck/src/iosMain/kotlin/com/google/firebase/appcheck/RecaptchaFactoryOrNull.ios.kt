/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck

import swiftPMImport.io.github.corneloaie.firebase.appcheck.FIRAppCheckProviderFactoryProtocol
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

internal actual fun AppCheckProviderFactory.recaptchaFactoryOrNull(): FIRAppCheckProviderFactoryProtocol? = (this as? RecaptchaAppCheckProviderFactory)?.ios
