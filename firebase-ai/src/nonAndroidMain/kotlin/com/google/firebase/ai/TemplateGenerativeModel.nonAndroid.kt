/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.ai

import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.TemplateTool
import com.google.firebase.ai.type.TemplateToolConfig
import io.github.corneloaie.firebase.ai.internal.NativeAI
import io.github.corneloaie.firebase.ai.internal.NativeRequestOptions
import io.github.corneloaie.firebase.ai.internal.catchAsync
import io.github.corneloaie.firebase.ai.internal.executeAutoFunction
import io.github.corneloaie.firebase.ai.internal.firebaseAIException
import io.github.corneloaie.firebase.ai.internal.templateGenerateContentRequest
import io.github.corneloaie.firebase.ai.internal.toGenerateContentResponse
import io.github.corneloaie.firebase.ai.internal.validate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@PublicPreviewAPI
public actual class TemplateGenerativeModel internal constructor(
    private val native: NativeAI,
    private val requestOptions: RequestOptions,
    private val tools: List<TemplateTool>?,
    private val toolConfig: TemplateToolConfig?,
) {
    private val options get() = NativeRequestOptions(requestOptions.timeout)

    public actual suspend fun generateContent(templateId: String, inputs: Map<String, Any>): GenerateContentResponse = generateContentWithHistory(templateId, inputs, null)

    internal actual suspend fun generateContentWithHistory(templateId: String, inputs: Map<String, Any>, history: List<Content>?): GenerateContentResponse = catchAsync {
        native.templateGenerateContent(templateId, templateGenerateContentRequest(inputs, history, tools, toolConfig), options).toGenerateContentResponse().validate()
    }

    public actual fun generateContentStream(templateId: String, inputs: Map<String, Any>): Flow<GenerateContentResponse> = generateContentWithHistoryStream(templateId, inputs, null)

    internal actual fun generateContentWithHistoryStream(templateId: String, inputs: Map<String, Any>, history: List<Content>?): Flow<GenerateContentResponse> = flow {
        val request = templateGenerateContentRequest(inputs, history, tools, toolConfig)
        emitAll(native.templateGenerateContentStream(templateId, request, options).map { it.toGenerateContentResponse().validate() })
    }.catch { throw firebaseAIException(it) }

    @PublicPreviewAPI
    public actual fun startChat(templateId: String, inputs: Map<String, Any>, history: List<Content>): TemplateChat = TemplateChat(this, templateId, inputs, history.toMutableList())

    internal actual fun hasFunction(call: FunctionCallPart): Boolean = tools.orEmpty().flatMap { it.autoFunctionDeclarations.orEmpty() }.any { it.name == call.name && it.functionReference != null }

    internal actual suspend fun executeFunction(call: FunctionCallPart): FunctionResponsePart {
        if (tools.isNullOrEmpty()) throw RuntimeException("No registered tools")
        val declaration = tools.flatMap { it.autoFunctionDeclarations.orEmpty() }.firstOrNull { it.name == call.name }
            ?: throw RuntimeException("No registered function named ${call.name}")
        return executeAutoFunction(call, declaration.name, declaration.inputSchema, declaration.outputSchema, declaration.functionReference)
    }
}
