/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JsModule("firebase/ai")
@file:JsNonModule

package io.github.corneloaie.firebase.ai.externals

import io.github.corneloaie.firebase.externals.FirebaseApp
import kotlin.js.Promise

// The parts of the Firebase JS SDK's `firebase/ai` this module uses. Requests are passed as plain objects in the shape
// of the REST API (see io.github.corneloaie.firebase.ai.internal.NativeAI); responses are read back as JSON.

public external interface AI

public external class GoogleAIBackend

public external class AgentPlatformBackend(location: String)

public external fun getAI(app: FirebaseApp, options: Any?): AI

public external interface GenerateContentResult {
    public val response: Any
}

public external interface GenerateContentStreamResult {
    public val stream: AsyncGenerator
}

/** A JavaScript async generator (`AsyncGenerator`). */
public external interface AsyncGenerator {
    public fun next(): Promise<IteratorResult>

    public fun `return`(value: Any?): Promise<IteratorResult>
}

public external interface IteratorResult {
    public val done: Boolean?
    public val value: Any?
}

public external interface GenerativeModel {
    public fun generateContent(request: Any): Promise<GenerateContentResult>

    public fun generateContentStream(request: Any): Promise<GenerateContentStreamResult>

    public fun countTokens(request: Any): Promise<Any>
}

public external fun getGenerativeModel(ai: AI, modelParams: Any, requestOptions: Any?): GenerativeModel

public external interface TemplateChatSession {
    public fun sendMessage(request: Any): Promise<GenerateContentResult>

    public fun sendMessageStream(request: Any): Promise<GenerateContentStreamResult>
}

public external interface TemplateGenerativeModel {
    public fun generateContent(request: Any): Promise<GenerateContentResult>

    public fun generateContentStream(request: Any): Promise<GenerateContentStreamResult>

    public fun startChat(params: Any): TemplateChatSession
}

public external fun getTemplateGenerativeModel(ai: AI, requestOptions: Any?): TemplateGenerativeModel

public external interface LiveSession {
    public val isClosed: Boolean

    public fun send(request: Any, turnComplete: Boolean): Promise<Any?>

    public fun sendTextRealtime(text: String): Promise<Any?>

    public fun sendAudioRealtime(blob: Any): Promise<Any?>

    public fun sendVideoRealtime(blob: Any): Promise<Any?>

    public fun sendFunctionResponses(functionResponses: Any): Promise<Any?>

    public fun receive(): AsyncGenerator

    public fun close(): Promise<Any?>
}

public external interface LiveGenerativeModel {
    public fun connect(sessionResumption: Any?): Promise<LiveSession>
}

public external fun getLiveGenerativeModel(ai: AI, modelParams: Any): LiveGenerativeModel
