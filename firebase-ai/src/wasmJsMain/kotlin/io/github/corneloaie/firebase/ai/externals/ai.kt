/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JsModule("firebase/ai")

package io.github.corneloaie.firebase.ai.externals

import io.github.corneloaie.firebase.externals.FirebaseApp
import kotlin.js.Promise

// The parts of the Firebase JS SDK's `firebase/ai` this module uses (the wasmJs mirror of jsMain's externals).
// Requests are passed as plain objects in the shape of the REST API; responses are read back as JSON.

public external interface AI : JsAny

public external class GoogleAIBackend : JsAny

public external class AgentPlatformBackend(location: String) : JsAny

public external fun getAI(app: FirebaseApp, options: JsAny?): AI

public external interface GenerateContentResult : JsAny {
    public val response: JsAny
}

public external interface GenerateContentStreamResult : JsAny {
    public val stream: AsyncGenerator
}

/** A JavaScript async generator (`AsyncGenerator`); its results are read with jsGet (`done`, `value`). */
public external interface AsyncGenerator : JsAny {
    public fun next(): Promise<JsAny>

    public fun `return`(value: JsAny?): Promise<JsAny>
}

public external interface GenerativeModel : JsAny {
    public fun generateContent(request: JsAny): Promise<GenerateContentResult>

    public fun generateContentStream(request: JsAny): Promise<GenerateContentStreamResult>

    public fun countTokens(request: JsAny): Promise<JsAny>
}

public external fun getGenerativeModel(ai: AI, modelParams: JsAny, requestOptions: JsAny?): GenerativeModel

public external interface TemplateChatSession : JsAny {
    public fun sendMessage(request: JsAny): Promise<GenerateContentResult>

    public fun sendMessageStream(request: JsAny): Promise<GenerateContentStreamResult>
}

public external interface TemplateGenerativeModel : JsAny {
    public fun generateContent(request: JsAny): Promise<GenerateContentResult>

    public fun generateContentStream(request: JsAny): Promise<GenerateContentStreamResult>

    public fun startChat(params: JsAny): TemplateChatSession
}

public external fun getTemplateGenerativeModel(ai: AI, requestOptions: JsAny?): TemplateGenerativeModel

public external interface LiveSession : JsAny {
    public val isClosed: Boolean

    public fun send(request: JsAny, turnComplete: Boolean): Promise<JsAny?>

    public fun sendTextRealtime(text: String): Promise<JsAny?>

    public fun sendAudioRealtime(blob: JsAny): Promise<JsAny?>

    public fun sendVideoRealtime(blob: JsAny): Promise<JsAny?>

    public fun sendFunctionResponses(functionResponses: JsAny): Promise<JsAny?>

    public fun receive(): AsyncGenerator

    public fun close(): Promise<JsAny?>
}

public external interface LiveGenerativeModel : JsAny {
    public fun connect(sessionResumption: JsAny?): Promise<LiveSession>
}

public external fun getLiveGenerativeModel(ai: AI, modelParams: JsAny): LiveGenerativeModel
