/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.ai.internal

import com.google.firebase.FirebaseApp
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.time.Duration

/**
 * The platform's AI Logic SDK for one app, backend and App Check token setting: the Firebase JS SDK's `firebase/ai` on
 * JS and wasmJs, and the Firebase AI Logic iOS SDK (`FirebaseAILogic`, Swift-only) through this module's Objective-C
 * wrapper package in `firebase-ai/apple` on Apple. The SDKs send the HTTP and WebSocket requests, with the app's API
 * key and its App Check and Auth tokens.
 *
 * Requests and responses cross as the JSON of the Firebase AI Logic REST API, which both SDKs' types follow (see
 * RequestJson.kt and ResponseJson.kt). Failures are thrown as [NativeAIError].
 */
internal expect class NativeAI(app: FirebaseApp, backend: GenerativeBackend, useLimitedUseAppCheckTokens: Boolean) {

    /**
     * Runs `generateContent` on [model] (a model name such as `gemini-2.5-flash`) with [request], a
     * `GenerateContentRequest` without its model: `contents`, and optionally `generationConfig`, `safetySettings`,
     * `tools`, `toolConfig` and `systemInstruction`. Returns the `GenerateContentResponse`, also one that reports a
     * blocked prompt or a candidate that stopped early (the caller checks it).
     */
    suspend fun generateContent(model: String, request: JsonObject, options: NativeRequestOptions): JsonObject

    /** [generateContent] as the stream of `GenerateContentResponse` chunks; cancelling the collection cancels the request. */
    fun generateContentStream(model: String, request: JsonObject, options: NativeRequestOptions): Flow<JsonObject>

    /** Runs `countTokens` on [model] for [request] (the JSON of [generateContent]) and returns the `CountTokensResponse`. */
    suspend fun countTokens(model: String, request: JsonObject, options: NativeRequestOptions): JsonObject

    /**
     * Runs the server prompt template [templateId] with [request]: `inputs` (the template's variables), and optionally
     * `history` (the conversation, ending with the new message of the user), `tools` (each with `templateFunctions`
     * or `googleMaps`) and `toolConfig`. Returns the `GenerateContentResponse`.
     */
    suspend fun templateGenerateContent(templateId: String, request: JsonObject, options: NativeRequestOptions): JsonObject

    /** [templateGenerateContent] as the stream of `GenerateContentResponse` chunks. */
    fun templateGenerateContentStream(templateId: String, request: JsonObject, options: NativeRequestOptions): Flow<JsonObject>

    /**
     * Opens a Live API session with [model] and [setup], the `setup` message of the Live API without its model:
     * optionally `generationConfig`, `tools`, `systemInstruction`, `inputAudioTranscription`,
     * `outputAudioTranscription`, `sessionResumption`, `contextWindowCompression` and `realtimeInputConfig`. Returns
     * once the server has completed the setup; fails with [NativeAIError.Kind.HANDSHAKE] when it does not.
     */
    suspend fun connect(model: String, setup: JsonObject, options: NativeRequestOptions): NativeLiveSession
}

/** The [com.google.firebase.ai.type.RequestOptions] the platform SDKs take. */
internal class NativeRequestOptions(val timeout: Duration)

/**
 * An open Live API session of the platform SDK. The messages of the server are delivered to the listener passed to
 * [start] as JSON objects with one key each: `serverContent`, `toolCall`, `toolCallCancellation`, `goAway`,
 * `sessionResumptionUpdate` (or another one the SDK does not know).
 */
internal expect class NativeLiveSession {
    /**
     * Starts delivering the messages of the server to [onMessage], until the session closes; then [onClose] is called
     * once, with the failure that closed it or null. Called once, right after the session opened.
     */
    fun start(onMessage: (JsonObject) -> Unit, onClose: (Throwable?) -> Unit)

    /** Sends `clientContent`: [turns] (contents) and whether the turn is complete. */
    suspend fun sendClientContent(turns: JsonArray, turnComplete: Boolean)

    /** Sends `realtimeInput`: one of `audio` and `video` (inline data), `text`, `activityStart` and `activityEnd`. */
    suspend fun sendRealtimeInput(input: JsonObject)

    /** Sends `toolResponse` with [functionResponses] (the `functionResponse` objects of the function response parts). */
    suspend fun sendToolResponse(functionResponses: JsonArray)

    /** Closes the session; [start]'s `onClose` is then called with null. */
    suspend fun close()
}
