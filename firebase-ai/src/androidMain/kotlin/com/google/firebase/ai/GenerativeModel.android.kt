/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.ai

import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.CountTokensResponse
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerateObjectResponse
import com.google.firebase.ai.type.JsonSchema
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.RequestOptions
import io.github.corneloaie.firebase.ai.internal.stub
import kotlinx.coroutines.flow.Flow

// Header stubs of the Android SDK's GenerativeModel and OnDeviceExtension (see utils.stripHeaderStubs).

public actual class GenerativeModel private constructor() {
    @PublicPreviewAPI
    public actual val onDeviceExtension: OnDeviceExtension? get() = stub()
    internal actual val requestOptions: RequestOptions get() = stub()

    public actual suspend fun generateContent(prompt: List<Content>): GenerateContentResponse = stub()
    public actual suspend fun generateContent(prompt: Content, vararg prompts: Content): GenerateContentResponse = stub()
    public actual suspend fun generateContent(prompt: String): GenerateContentResponse = stub()
    public actual fun generateContentStream(prompt: List<Content>): Flow<GenerateContentResponse> = stub()
    public actual fun generateContentStream(prompt: Content, vararg prompts: Content): Flow<GenerateContentResponse> = stub()
    public actual fun generateContentStream(prompt: String): Flow<GenerateContentResponse> = stub()
    public actual suspend fun <T : Any> generateObject(jsonSchema: JsonSchema<T>, prompt: Content, vararg prompts: Content): GenerateObjectResponse<T> = stub()
    public actual suspend fun <T : Any> generateObject(jsonSchema: JsonSchema<T>, prompt: String): GenerateObjectResponse<T> = stub()
    public actual fun startChat(history: List<Content>): Chat = stub()
    public actual suspend fun countTokens(prompt: List<Content>): CountTokensResponse = stub()
    public actual suspend fun countTokens(prompt: Content, vararg prompts: Content): CountTokensResponse = stub()
    public actual suspend fun countTokens(prompt: String): CountTokensResponse = stub()
    internal actual fun hasFunction(call: FunctionCallPart): Boolean = stub()
    internal actual suspend fun executeFunction(call: FunctionCallPart): FunctionResponsePart = stub()
}

@PublicPreviewAPI
public actual class OnDeviceExtension private constructor() {
    public actual suspend fun checkStatus(): OnDeviceModelStatus = stub()
    public actual fun download(): Flow<DownloadStatus> = stub()
    public actual suspend fun warmUp(): Unit = stub()
    public actual suspend fun getModelName(): String = stub()
}
