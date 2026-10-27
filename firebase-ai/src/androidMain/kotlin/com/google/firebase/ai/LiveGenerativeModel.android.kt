/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.ai

import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.SessionResumptionConfig
import io.github.corneloaie.firebase.ai.internal.stub

// Header stubs of the Android SDK's LiveGenerativeModel (see utils.stripHeaderStubs).

@PublicPreviewAPI
public actual class LiveGenerativeModel private constructor() {
    @JvmOverloads
    public actual suspend fun connect(sessionResumption: SessionResumptionConfig?): LiveSession = stub()
}
