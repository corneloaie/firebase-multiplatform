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
import dev.gitlive.firebase.externals.jsGet
import dev.gitlive.firebase.externals.json
import dev.gitlive.firebase.externals.jsonParse
import dev.gitlive.firebase.externals.jsonStringify
import dev.gitlive.firebase.externals.toKotlin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsException
import kotlin.js.Promise

/**
 * The Firebase JS SDK's `firebase/ai` (the wasmJs mirror of jsMain's NativeAI). Requests are passed as JavaScript
 * objects parsed from the REST JSON (the SDK's request types are that JSON), responses read back with `JSON.stringify`,
 * which leaves out the helper functions the SDK attaches to them.
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
        jsCall { model(model, options).generateContent(request.toJs()) }.awaitResult().response.toJsonObject()

    actual fun generateContentStream(model: String, request: JsonObject, options: NativeRequestOptions): Flow<JsonObject> =
        streamOf { jsCall { model(model, options).generateContentStream(request.toJs()) }.awaitResult() }

    actual suspend fun countTokens(model: String, request: JsonObject, options: NativeRequestOptions): JsonObject =
        jsCall { model(model, options).countTokens(request.toJs()) }.awaitResult().toJsonObject()

    actual suspend fun templateGenerateContent(templateId: String, request: JsonObject, options: NativeRequestOptions): JsonObject {
        val templates = getTemplateGenerativeModel(ai, options.toJs())
        val history = request.objects("history")
        return jsCall {
            if (history.isNullOrEmpty()) {
                templates.generateContent(templateRequest(templateId, request).toJs())
            } else {
                templates.startChat(templateChatParams(templateId, request, history).toJs()).sendMessage(history.last().partsToJs())
            }
        }.awaitResult().response.toJsonObject()
    }

    actual fun templateGenerateContentStream(templateId: String, request: JsonObject, options: NativeRequestOptions): Flow<JsonObject> = streamOf {
        val templates = getTemplateGenerativeModel(ai, options.toJs())
        val history = request.objects("history")
        jsCall {
            if (history.isNullOrEmpty()) {
                templates.generateContentStream(templateRequest(templateId, request).toJs())
            } else {
                templates.startChat(templateChatParams(templateId, request, history).toJs()).sendMessageStream(history.last().partsToJs())
            }
        }.awaitResult()
    }

    actual suspend fun connect(model: String, setup: JsonObject, options: NativeRequestOptions): NativeLiveSession {
        val liveModel = getLiveGenerativeModel(ai, liveModelParams(model, setup).toJs())
        val session = jsCall(handshake = true) { liveModel.connect(setup["sessionResumption"]?.toJs()) }.awaitResult(handshake = true)
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
                onFulfilled = { result ->
                    if (jsGet(result, "done")?.toKotlin() == true) {
                        onClose(null)
                    } else {
                        jsGet(result, "value")?.let { liveServerMessage(it) }?.let(onMessage)
                        next()
                    }
                    null
                },
                onRejected = { error ->
                    onClose(error.toNativeAIError(null))
                    null
                },
            )
        }
        next()
    }

    actual suspend fun sendClientContent(turns: JsonArray, turnComplete: Boolean) {
        val content = turns.firstOrNull() as? JsonObject ?: return
        jsCall { js.send(content.partsToJs(), turnComplete) }.awaitResult()
    }

    actual suspend fun sendRealtimeInput(input: JsonObject) {
        when {
            input.containsKey("audio") -> jsCall { js.sendAudioRealtime(input.getValue("audio").toJs()) }.awaitResult()
            input.containsKey("video") -> jsCall { js.sendVideoRealtime(input.getValue("video").toJs()) }.awaitResult()
            input.containsKey("text") -> jsCall { js.sendTextRealtime(input.string("text") ?: "") }.awaitResult()
            else -> sendMessage(buildJsonObject { put("realtimeInput", input) })
        }
    }

    actual suspend fun sendToolResponse(functionResponses: JsonArray) {
        jsCall { js.sendFunctionResponses(functionResponses.toJs()) }.awaitResult()
    }

    actual suspend fun close() {
        jsCall { js.close() }.awaitResult()
    }

    private fun sendMessage(message: JsonObject) {
        if (js.isClosed) throw NativeAIError(NativeAIError.Kind.INVALID_REQUEST, "This LiveSession has been closed and cannot be used.")
        val handler = jsGet(js, "_webSocketHandler")
        if (handler == null || !hasFunction(handler, "send")) {
            throw NativeAIError(NativeAIError.Kind.INVALID_REQUEST, "This version of the Firebase JS SDK cannot send ${message.keys.first()} messages")
        }
        callSend(handler, message.toString())
    }
}

private fun hasFunction(target: JsAny, name: String): Boolean = js("typeof target[name] === 'function'")

private fun callSend(handler: JsAny, data: String) {
    js("handler.send(data)")
}

private fun JsonElement.toJs(): JsAny = jsonParse(toString())!!

private fun JsAny?.toJsonObject(): JsonObject = parseJsonObject(this?.let { jsonStringify(it) })

private fun NativeRequestOptions.toJs(): JsAny = json("timeout" to timeout.inWholeMilliseconds.toDouble())

/** The parts of a content, as the JS SDK takes a new message (`string | Array<string | Part>`). */
private fun JsonObject.partsToJs(): JsAny = (array("parts") ?: JsonArray(emptyList())).toJs()

/** A template request without history: the JS SDK takes its inputs as `templateVariables`. */
private fun templateRequest(templateId: String, request: JsonObject): JsonObject = buildJsonObject {
    put("templateId", JsonPrimitive(templateId))
    put("templateVariables", request["inputs"] ?: JsonObject(emptyMap()))
    request["toolConfig"]?.let { put("toolConfig", it) }
}

/**
 * A template chat over [history] but its last content (the new message): the JS SDK runs a template with history only as
 * a chat. Its template tools take `functionDeclarations` with the JSON schema of their input as `parameters`.
 */
private fun templateChatParams(templateId: String, request: JsonObject, history: List<JsonObject>): JsonObject = buildJsonObject {
    put("templateId", JsonPrimitive(templateId))
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
    put("model", JsonPrimitive(model))
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
private fun liveServerMessage(message: JsAny): JsonObject? {
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
    val stream: AsyncGenerator = start().stream
    var completed = false
    try {
        while (true) {
            val result = jsCall { stream.next() }.awaitResult()
            if (jsGet(result, "done")?.toKotlin() == true) break
            emit(jsGet(result, "value").toJsonObject())
        }
        completed = true
    } finally {
        // Stops the request when the collection ends early (cancelled or failed).
        if (!completed) stream.`return`(null)
    }
}

private inline fun <T> jsCall(handshake: Boolean = false, block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    throw e.toNativeAIError(handshake)
}

private suspend fun <T : JsAny?> Promise<T>.awaitResult(handshake: Boolean = false): T = suspendCancellableCoroutine { continuation ->
    then(
        onFulfilled = {
            continuation.resume(it)
            null
        },
        onRejected = {
            continuation.resumeWithException(it.toNativeAIError(null, handshake))
            null
        },
    )
}

private fun Throwable.toNativeAIError(handshake: Boolean = false): Throwable {
    if (this is NativeAIError || this is CancellationException) return this
    return (this as? JsException)?.thrownValue?.toNativeAIError(this, handshake)
        ?: NativeAIError(if (handshake) NativeAIError.Kind.HANDSHAKE else NativeAIError.Kind.UNKNOWN, message ?: toString(), cause = this)
}

/**
 * The [NativeAIError] of an error of the JS SDK: an `AIError` with a `code` and, for an error response of the server,
 * `customErrorData` with its HTTP status and gRPC error details; a timeout is an `AbortError`.
 */
private fun JsAny.toNativeAIError(thrown: Throwable?, handshake: Boolean = false): Throwable {
    val message = (jsGet(this, "message")?.toKotlin() as? String) ?: thrown?.message ?: jsonStringify(this)
    val cause = thrown ?: Exception(message)
    if (jsGet(this, "name")?.toKotlin() == "AbortError") return NativeAIError(NativeAIError.Kind.TIMEOUT, message, cause = cause)
    val code = jsGet(this, "code")?.toKotlin() as? String
        ?: return NativeAIError(if (handshake) NativeAIError.Kind.HANDSHAKE else NativeAIError.Kind.UNKNOWN, message, cause = cause)
    val data = jsGet(this, "customErrorData")
    val status = (data?.let { jsGet(it, "status") }?.toKotlin() as? Number)?.toInt()
    val details = data?.let { jsGet(it, "errorDetails") }?.let { runCatching { Json.parseToJsonElement(jsonStringify(it)) }.getOrNull() as? JsonArray }
        ?.filterIsInstance<JsonObject>().orEmpty()
    val kind = when (code) {
        "fetch-error" -> NativeAIError.Kind.SERVER
        // The server reported SERVICE_DISABLED for the AI Logic API (the details say so).
        "api-not-enabled" -> NativeAIError.Kind.SERVER
        "response-error" -> if (handshake) NativeAIError.Kind.HANDSHAKE else NativeAIError.Kind.SERVER
        "parse-failed" -> NativeAIError.Kind.SERIALIZATION
        "invalid-content", "invalid-schema", "no-api-key", "no-app-id", "no-model", "no-project-id", "unsupported", "request-error", "session-closed" -> NativeAIError.Kind.INVALID_REQUEST
        else -> if (handshake) NativeAIError.Kind.HANDSHAKE else NativeAIError.Kind.UNKNOWN
    }
    return NativeAIError(kind, message, status, details, cause)
}
