/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.ai

import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.CountTokensResponse
import com.google.firebase.ai.type.FirebaseAIOnDeviceNotAvailableException
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerateObjectResponse
import com.google.firebase.ai.type.GenerationConfig
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.GenerativeBackendEnum
import com.google.firebase.ai.type.JsonSchema
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.SafetySetting
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.ToolConfig
import com.google.firebase.ai.type.content
import io.github.corneloaie.firebase.UnsupportedOnApple
import io.github.corneloaie.firebase.UnsupportedOnJs
import io.github.corneloaie.firebase.ai.internal.NativeAI
import io.github.corneloaie.firebase.ai.internal.NativeRequestOptions
import io.github.corneloaie.firebase.ai.internal.catchAsync
import io.github.corneloaie.firebase.ai.internal.checkSafetySettingsFor
import io.github.corneloaie.firebase.ai.internal.countTokensRequest
import io.github.corneloaie.firebase.ai.internal.executeAutoFunction
import io.github.corneloaie.firebase.ai.internal.firebaseAIException
import io.github.corneloaie.firebase.ai.internal.generateContentRequest
import io.github.corneloaie.firebase.ai.internal.toCountTokensResponse
import io.github.corneloaie.firebase.ai.internal.toGenerateContentResponse
import io.github.corneloaie.firebase.ai.internal.validate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject

/**
 * A model of the cloud backend, over the platform SDK. The platforms other than Android have no on-device model (as
 * Android without the `firebase-ai-ondevice` library): [OnDeviceConfig] modes that prefer one of the two fall back to
 * the cloud, `ONLY_ON_DEVICE` fails with [FirebaseAIOnDeviceNotAvailableException], and [onDeviceExtension] is null.
 */
public actual class GenerativeModel internal constructor(
    private val native: NativeAI,
    private val backend: GenerativeBackend,
    private val modelName: String,
    private val generationConfig: GenerationConfig?,
    private val safetySettings: List<SafetySetting>?,
    private val tools: List<Tool>,
    private val toolConfig: ToolConfig?,
    private val systemInstruction: Content?,
    internal actual val requestOptions: RequestOptions,
    private val onDeviceConfig: OnDeviceConfig,
) {
    @PublicPreviewAPI
    public actual val onDeviceExtension: OnDeviceExtension? = null

    private val options get() = NativeRequestOptions(requestOptions.timeout)

    private val onDeviceOnly get() = onDeviceConfig.mode == InferenceMode.ONLY_ON_DEVICE

    private fun request(prompt: List<Content>, config: GenerationConfig? = generationConfig): JsonObject {
        checkSafetySettingsFor(backend.backend == GenerativeBackendEnum.GOOGLE_AI, safetySettings)
        return generateContentRequest(prompt, safetySettings, config, tools, toolConfig, systemInstruction)
    }

    private fun onDeviceNotAvailable() = FirebaseAIOnDeviceNotAvailableException.create("On-device model is not available")

    public actual suspend fun generateContent(prompt: List<Content>): GenerateContentResponse = catchAsync {
        if (onDeviceOnly) throw onDeviceNotAvailable()
        native.generateContent(modelName, request(prompt), options).toGenerateContentResponse().validate()
    }

    public actual suspend fun generateContent(prompt: Content, vararg prompts: Content): GenerateContentResponse = generateContent(listOf(prompt, *prompts))

    public actual suspend fun generateContent(prompt: String): GenerateContentResponse = generateContent(listOf(content { text(prompt) }))

    public actual fun generateContentStream(prompt: List<Content>): Flow<GenerateContentResponse> = flow {
        if (onDeviceOnly) throw onDeviceNotAvailable()
        emitAll(native.generateContentStream(modelName, request(prompt), options).map { it.toGenerateContentResponse().validate() })
    }.catch { throw firebaseAIException(it) }

    public actual fun generateContentStream(prompt: Content, vararg prompts: Content): Flow<GenerateContentResponse> = generateContentStream(listOf(prompt, *prompts))

    public actual fun generateContentStream(prompt: String): Flow<GenerateContentResponse> = generateContentStream(listOf(content { text(prompt) }))

    public actual suspend fun <T : Any> generateObject(jsonSchema: JsonSchema<T>, prompt: Content, vararg prompts: Content): GenerateObjectResponse<T> = catchAsync {
        if (onDeviceOnly) throw onDeviceNotAvailable()
        val config = (generationConfig?.toBuilder() ?: GenerationConfig.builder())
            .setResponseSchemaJson(jsonSchema)
            .setResponseMimeType("application/json")
            .build()
        val response = native.generateContent(modelName, request(listOf(prompt, *prompts), config), options).toGenerateContentResponse().validate()
        GenerateObjectResponse(response, jsonSchema)
    }

    public actual suspend fun <T : Any> generateObject(jsonSchema: JsonSchema<T>, prompt: String): GenerateObjectResponse<T> = generateObject(jsonSchema, content { text(prompt) })

    public actual fun startChat(history: List<Content>): Chat = Chat(this, history.toMutableList())

    public actual suspend fun countTokens(prompt: List<Content>): CountTokensResponse = catchAsync {
        if (onDeviceOnly) throw onDeviceNotAvailable()
        native.countTokens(modelName, countTokensRequest(prompt, generationConfig, tools, systemInstruction), options).toCountTokensResponse()
    }

    public actual suspend fun countTokens(prompt: Content, vararg prompts: Content): CountTokensResponse = countTokens(listOf(prompt, *prompts))

    public actual suspend fun countTokens(prompt: String): CountTokensResponse = countTokens(listOf(content { text(prompt) }))

    internal actual fun hasFunction(call: FunctionCallPart): Boolean = tools.flatMap { it.autoFunctionDeclarations.orEmpty() }.any { it.name == call.name && it.functionReference != null }

    internal actual suspend fun executeFunction(call: FunctionCallPart): FunctionResponsePart {
        if (tools.isEmpty()) throw RuntimeException("No registered tools")
        val declaration = tools.flatMap { it.autoFunctionDeclarations.orEmpty() }.firstOrNull { it.name == call.name }
            ?: throw RuntimeException("No registered function named ${call.name}")
        return executeAutoFunction(call, declaration.name, declaration.inputSchema, declaration.outputSchema, declaration.functionReference)
    }
}

/**
 * The on-device model of a [GenerativeModel]. The platforms other than Android have none (as Android without the
 * `firebase-ai-ondevice` library), so [GenerativeModel.onDeviceExtension] is always null there and no instance exists;
 * its members report the model as unavailable.
 */
@PublicPreviewAPI
public actual class OnDeviceExtension private constructor() {
    @UnsupportedOnJs
    @UnsupportedOnApple
    public actual suspend fun checkStatus(): OnDeviceModelStatus = OnDeviceModelStatus.UNAVAILABLE

    @UnsupportedOnJs
    @UnsupportedOnApple
    public actual fun download(): Flow<DownloadStatus> = flow { throw FirebaseAIOnDeviceNotAvailableException.create("On-device model is not available") }

    @UnsupportedOnJs
    @UnsupportedOnApple
    public actual suspend fun warmUp() {}

    @UnsupportedOnJs
    @UnsupportedOnApple
    public actual suspend fun getModelName(): String = throw UnsupportedOperationException("On-device model is not available")
}
