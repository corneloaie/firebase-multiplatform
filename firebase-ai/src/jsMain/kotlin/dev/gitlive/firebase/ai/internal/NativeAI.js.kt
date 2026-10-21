/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.ai.internal

import com.google.firebase.FirebaseApp
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.GenerativeBackendEnum
import dev.gitlive.firebase.ai.externals.AI
import dev.gitlive.firebase.ai.externals.AgentPlatformBackend
import dev.gitlive.firebase.ai.externals.AsyncGenerator
import dev.gitlive.firebase.ai.externals.GenerateContentStreamResult
import dev.gitlive.firebase.ai.externals.GoogleAIBackend
import dev.gitlive.firebase.ai.externals.LiveSession
import dev.gitlive.firebase.ai.externals.getAI
import dev.gitlive.firebase.ai.externals.getGenerativeModel
import dev.gitlive.firebase.ai.externals.getLiveGenerativeModel
import dev.gitlive.firebase.ai.externals.getTemplateGenerativeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlin.js.json

/**
 * The Firebase JS SDK's `firebase/ai`. Requests are passed as JavaScript objects parsed from the REST JSON (the SDK's
 * request types are that JSON), responses read back with `JSON.stringify`, which leaves out the helper functions the
 * SDK attaches to them.
 */
internal actual class NativeAI actual constructor(app: FirebaseApp, backend: GenerativeBackend, useLimitedUseAppCheckTokens: Boolean) {

    private val ai: AI = getAI(
        app.js,
        json(
            "backend" to when (backend.backend) {
                GenerativeBackendEnum.GOOGLE_AI -> GoogleAIBackend()
                GenerativeBackendEnum.AGENT_PLATFORM -> AgentPlatformBackend(backend.location)
            },
            "useLimitedUseAppCheckTokens" to useLimitedUseAppCheckTokens,
        ),
    )

    private fun model(model: String, options: NativeRequestOptions) = getGenerativeModel(ai, json("model" to model), options.toJs())

    actual suspend fun generateContent(model: String, request: JsonObject, options: NativeRequestOptions): JsonObject =
        jsCall { model(model, options).generateContent(request.toJs()).await() }.response.toJsonObject()

    actual fun generateContentStream(model: String, request: JsonObject, options: NativeRequestOptions): Flow<JsonObject> =
        streamOf { model(model, options).generateContentStream(request.toJs()).await() }

    actual suspend fun countTokens(model: String, request: JsonObject, options: NativeRequestOptions): JsonObject =
        jsCall { model(model, options).countTokens(request.toJs()).await() }.toJsonObject()

    actual suspend fun templateGenerateContent(templateId: String, request: JsonObject, options: NativeRequestOptions): JsonObject {
        val templates = getTemplateGenerativeModel(ai, options.toJs())
        val history = request.objects("history")
        return jsCall {
            if (history.isNullOrEmpty()) {
                templates.generateContent(templateRequest(templateId, request).toJs()).await()
            } else {
                templates.startChat(templateChatParams(templateId, request, history).toJs()).sendMessage(history.last().partsToJs()).await()
            }
        }.response.toJsonObject()
    }

    actual fun templateGenerateContentStream(templateId: String, request: JsonObject, options: NativeRequestOptions): Flow<JsonObject> = streamOf {
        val templates = getTemplateGenerativeModel(ai, options.toJs())
        val history = request.objects("history")
        if (history.isNullOrEmpty()) {
            templates.generateContentStream(templateRequest(templateId, request).toJs()).await()
        } else {
            templates.startChat(templateChatParams(templateId, request, history).toJs()).sendMessageStream(history.last().partsToJs()).await()
        }
    }

    actual suspend fun connect(model: String, setup: JsonObject, options: NativeRequestOptions): NativeLiveSession {
        val liveModel = getLiveGenerativeModel(ai, liveModelParams(model, setup).toJs())
        val session = try {
            liveModel.connect(setup["sessionResumption"]?.toJs() ?: undefined).await()
        } catch (e: Throwable) {
            throw e.toNativeAIError(handshake = true)
        }
        return NativeLiveSession(session)
    }
}

/**
 * A Live API session of the JS SDK. The JS SDK has no messages for the activity signals; they are sent through its
 * WebSocket handler (`_webSocketHandler`, which every message of the session goes through) as the Android SDK sends them.
 */
internal actual class NativeLiveSession(private val js: LiveSession) {

    actual fun start(onMessage: (JsonObject) -> Unit, onClose: (Throwable?) -> Unit) {
        val messages = try {
            js.receive()
        } catch (e: Throwable) {
            onClose(e.toNativeAIError())
            return
        }
        fun next() {
            messages.next().then(
                { result ->
                    if (result.done == true) {
                        onClose(null)
                    } else {
                        result.value?.let { liveServerMessage(it) }?.let(onMessage)
                        next()
                    }
                },
                { error -> onClose(error.toNativeAIError()) },
            )
        }
        next()
    }

    actual suspend fun sendClientContent(turns: JsonArray, turnComplete: Boolean) {
        val content = turns.firstOrNull() as? JsonObject ?: return
        jsCall { js.send(content.partsToJs(), turnComplete).await() }
    }

    actual suspend fun sendRealtimeInput(input: JsonObject) {
        jsCall {
            when {
                input.containsKey("audio") -> js.sendAudioRealtime(input.getValue("audio").toJs()!!).await()
                input.containsKey("video") -> js.sendVideoRealtime(input.getValue("video").toJs()!!).await()
                input.containsKey("text") -> js.sendTextRealtime(input.string("text") ?: "").await()
                else -> sendMessage(buildJsonObject { put("realtimeInput", input) })
            }
        }
    }

    actual suspend fun sendToolResponse(functionResponses: JsonArray) {
        jsCall { js.sendFunctionResponses(functionResponses.toJs()!!).await() }
    }

    actual suspend fun close() {
        jsCall { js.close().await() }
    }

    private fun sendMessage(message: JsonObject) {
        if (js.isClosed) throw NativeAIError(NativeAIError.Kind.INVALID_REQUEST, "This LiveSession has been closed and cannot be used.")
        val handler = js.asDynamic()._webSocketHandler
        if (handler == null || jsTypeOf(handler.send) != "function") {
            throw NativeAIError(NativeAIError.Kind.INVALID_REQUEST, "This version of the Firebase JS SDK cannot send ${message.keys.first()} messages")
        }
        handler.send(message.toString())
    }
}

private fun jsTypeOf(value: dynamic): String = js("typeof value") as String

private fun JsonElement.toJs(): dynamic = JSON.parse(toString())

private fun Any?.toJsonObject(): JsonObject = parseJsonObject(this?.let { JSON.stringify(it) })

private fun NativeRequestOptions.toJs(): dynamic = json("timeout" to timeout.inWholeMilliseconds.toDouble())

/** The parts of a content, as the JS SDK takes a new message (`string | Array<string | Part>`). */
private fun JsonObject.partsToJs(): dynamic = (array("parts") ?: JsonArray(emptyList())).toJs()

/** A template request without history: the JS SDK takes its inputs as `templateVariables`. */
private fun templateRequest(templateId: String, request: JsonObject): JsonObject = buildJsonObject {
    put("templateId", kotlinx.serialization.json.JsonPrimitive(templateId))
    put("templateVariables", request["inputs"] ?: JsonObject(emptyMap()))
    request["toolConfig"]?.let { put("toolConfig", it) }
}

/**
 * A template chat over [history] but its last content (the new message): the JS SDK runs a template with history only as
 * a chat. Its template tools take `functionDeclarations` with the JSON schema of their input as `parameters`.
 */
private fun templateChatParams(templateId: String, request: JsonObject, history: List<JsonObject>): JsonObject = buildJsonObject {
    put("templateId", kotlinx.serialization.json.JsonPrimitive(templateId))
    put("templateVariables", request["inputs"] ?: JsonObject(emptyMap()))
    put("history", JsonArray(history.dropLast(1)))
    request.objects("tools")?.let { tools ->
        put(
            "tools",
            JsonArray(
                tools.mapNotNull { tool ->
                    tool.objects("templateFunctions")?.takeIf { it.isNotEmpty() }?.let { functions ->
                        buildJsonObject {
                            put(
                                "functionDeclarations",
                                JsonArray(
                                    functions.map { function ->
                                        buildJsonObject {
                                            put("name", function["name"]!!)
                                            function["inputSchema"]?.let { put("parameters", it) }
                                        }
                                    },
                                ),
                            )
                        }
                    }
                },
            ),
        )
    }
    request["toolConfig"]?.let { put("toolConfig", it) }
}

/**
 * The JS SDK's Live model parameters for [setup]: it takes the audio transcription and context window compression
 * settings in the generation config and moves them to the setup itself. It has no realtime input configuration.
 */
private fun liveModelParams(model: String, setup: JsonObject): JsonObject = buildJsonObject {
    put("model", kotlinx.serialization.json.JsonPrimitive(model))
    put(
        "generationConfig",
        buildJsonObject {
            setup.obj("generationConfig")?.forEach { (key, value) -> put(key, value) }
            listOf("inputAudioTranscription", "outputAudioTranscription", "contextWindowCompression").forEach { key ->
                setup[key]?.let { put(key, it) }
            }
        },
    )
    setup["tools"]?.let { put("tools", it) }
    setup["systemInstruction"]?.let { put("systemInstruction", it) }
}

/** A message of the JS SDK's `LiveSession.receive()` (`{type, ...}`) as the server's message (`{[type]: {...}}`). */
private fun liveServerMessage(message: Any): JsonObject? {
    val json = message.toJsonObject()
    val type = json.string("type") ?: return null
    val body = JsonObject(json - "type")
    return when (type) {
        // The JS SDK has parsed the protobuf duration into seconds.
        "goingAwayNotice" -> buildJsonObject { put("goAway", body) }
        else -> buildJsonObject { put(type, body) }
    }
}

private fun streamOf(start: suspend () -> GenerateContentStreamResult): Flow<JsonObject> = flow {
    val stream: AsyncGenerator = jsCall { start() }.stream
    var completed = false
    try {
        while (true) {
            val result = jsCall { stream.next().await() }
            if (result.done == true) break
            emit(result.value.toJsonObject())
        }
        completed = true
    } finally {
        // Stops the request when the collection ends early (cancelled or failed).
        if (!completed) stream.`return`(undefined)
    }
}

private inline fun <T> jsCall(block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    throw e.toNativeAIError()
}

/**
 * The [NativeAIError] of an error of the JS SDK: an `AIError` with a `code` and, for an error response of the server,
 * `customErrorData` with its HTTP status and gRPC error details; a timeout is an `AbortError`.
 */
private fun Throwable.toNativeAIError(handshake: Boolean = false): Throwable {
    if (this is NativeAIError || this is CancellationException) return this
    val js = asDynamic()
    val message = message ?: toString()
    if (js.name == "AbortError") return NativeAIError(NativeAIError.Kind.TIMEOUT, message, cause = this)
    val code = js.code as? String ?: return NativeAIError(if (handshake) NativeAIError.Kind.HANDSHAKE else NativeAIError.Kind.UNKNOWN, message, cause = this)
    val data = js.customErrorData
    val status = (data?.status as? Number)?.toInt()
    val details = (data?.errorDetails as? Any)?.let { (it.toJsonElementOrNull() as? JsonArray)?.filterIsInstance<JsonObject>() }.orEmpty()
    val kind = when (code) {
        "fetch-error" -> NativeAIError.Kind.SERVER
        // The server reported SERVICE_DISABLED for the AI Logic API (the details say so).
        "api-not-enabled" -> NativeAIError.Kind.SERVER
        "response-error" -> if (handshake) NativeAIError.Kind.HANDSHAKE else NativeAIError.Kind.SERVER
        "parse-failed" -> NativeAIError.Kind.SERIALIZATION
        "invalid-content", "invalid-schema", "no-api-key", "no-app-id", "no-model", "no-project-id", "unsupported", "request-error", "session-closed" -> NativeAIError.Kind.INVALID_REQUEST
        else -> if (handshake) NativeAIError.Kind.HANDSHAKE else NativeAIError.Kind.UNKNOWN
    }
    return NativeAIError(kind, message, status, details, this)
}

private fun Any.toJsonElementOrNull(): JsonElement? = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(JSON.stringify(this)) }.getOrNull()
