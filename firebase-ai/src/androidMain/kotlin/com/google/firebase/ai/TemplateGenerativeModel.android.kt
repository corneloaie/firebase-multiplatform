/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.ai

import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.PublicPreviewAPI
import io.github.corneloaie.firebase.ai.internal.stub
import kotlinx.coroutines.flow.Flow

// Header stubs of the Android SDK's TemplateGenerativeModel (see utils.stripHeaderStubs).

@PublicPreviewAPI
public actual class TemplateGenerativeModel private constructor() {
    public actual suspend fun generateContent(templateId: String, inputs: Map<String, Any>): GenerateContentResponse = stub()
    internal actual suspend fun generateContentWithHistory(templateId: String, inputs: Map<String, Any>, history: List<Content>?): GenerateContentResponse = stub()
    public actual fun generateContentStream(templateId: String, inputs: Map<String, Any>): Flow<GenerateContentResponse> = stub()
    internal actual fun generateContentWithHistoryStream(templateId: String, inputs: Map<String, Any>, history: List<Content>?): Flow<GenerateContentResponse> = stub()

    @PublicPreviewAPI
    public actual fun startChat(templateId: String, inputs: Map<String, Any>, history: List<Content>): TemplateChat = stub()
    internal actual fun hasFunction(call: FunctionCallPart): Boolean = stub()
    internal actual suspend fun executeFunction(call: FunctionCallPart): FunctionResponsePart = stub()
}
