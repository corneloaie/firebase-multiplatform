/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.ai.internal

import com.google.firebase.FirebaseApp
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.GenerativeBackendEnum
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import swiftPMImport.io.github.corneloaie.firebase.ai.FAIBridge
import swiftPMImport.io.github.corneloaie.firebase.ai.FAICancellable
import swiftPMImport.io.github.corneloaie.firebase.ai.FAILiveSession
import kotlin.coroutines.resume

/*
 * The integers and keys of the wrapper's API (see apple/FirebaseAILogicObjC/Sources/AIBridge.swift).
 */
private const val BACKEND_GOOGLE_AI = 0L
private const val BACKEND_AGENT_PLATFORM = 1L
private const val STATUS_KEY = "status"
private const val DETAILS_KEY = "details"

/**
 * The Firebase AI Logic iOS SDK, through the Objective-C wrapper package in `apple/FirebaseAILogicObjC` (the SDK is
 * Swift-only). Requests and responses cross as JSON text, failures as `NSError`s whose code is the [NativeAIError.Kind];
 * cancelling a call cancels the SDK's task.
 */
internal actual class NativeAI actual constructor(app: FirebaseApp, backend: GenerativeBackend, useLimitedUseAppCheckTokens: Boolean) {

    private val ios = FAIBridge(
        appName = app.name,
        backend = when (backend.backend) {
            GenerativeBackendEnum.GOOGLE_AI -> BACKEND_GOOGLE_AI
            GenerativeBackendEnum.AGENT_PLATFORM -> BACKEND_AGENT_PLATFORM
        },
        location = backend.location,
        useLimitedUseAppCheckTokens = useLimitedUseAppCheckTokens,
    )

    actual suspend fun generateContent(model: String, request: JsonObject, options: NativeRequestOptions): JsonObject = call { completion ->
        ios.generateContent(model, request.toString(), options.seconds, completion)
    }

    actual fun generateContentStream(model: String, request: JsonObject, options: NativeRequestOptions): Flow<JsonObject> = stream { onChunk, completion ->
        ios.generateContentStream(model, request.toString(), options.seconds, onChunk, completion)
    }

    actual suspend fun countTokens(model: String, request: JsonObject, options: NativeRequestOptions): JsonObject = call { completion ->
        ios.countTokens(model, request.toString(), options.seconds, completion)
    }

    actual suspend fun templateGenerateContent(templateId: String, request: JsonObject, options: NativeRequestOptions): JsonObject = call { completion ->
        ios.templateGenerateContent(templateId, request.toString(), options.seconds, completion)
    }

    actual fun templateGenerateContentStream(templateId: String, request: JsonObject, options: NativeRequestOptions): Flow<JsonObject> = stream { onChunk, completion ->
        ios.templateGenerateContentStream(templateId, request.toString(), options.seconds, onChunk, completion)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    actual suspend fun connect(model: String, setup: JsonObject, options: NativeRequestOptions): NativeLiveSession = suspendCancellableCoroutine { continuation ->
        val cancellable = ios.connect(model, setup.toString(), options.seconds) { session, error ->
            if (session != null) {
                // A session that arrives after the coroutine was cancelled is closed.
                continuation.resume(NativeLiveSession(session)) { _, _, _ -> session.close {} }
            } else {
                continuation.resumeWith(Result.failure(error.toNativeAIError()))
            }
        }
        continuation.invokeOnCancellation { cancellable.cancel() }
    }
}

/**
 * An open Live API session of the iOS SDK. The wrapper calls back on the SDK's threads; what the callbacks given to
 * [start] throw is dropped (an exception cannot cross into Swift), and so is a message that is not a JSON object.
 */
internal actual class NativeLiveSession internal constructor(private val ios: FAILiveSession) {

    actual fun start(onMessage: (JsonObject) -> Unit, onClose: (Throwable?) -> Unit) {
        ios.start(
            { message -> runCatching { onMessage(message.toJsonObject()) } },
            { error -> runCatching { onClose(error?.toNativeAIError()) } },
        )
    }

    actual suspend fun sendClientContent(turns: JsonArray, turnComplete: Boolean): Unit = send { completion ->
        ios.sendClientContent(turns.toString(), turnComplete, completion)
    }

    actual suspend fun sendRealtimeInput(input: JsonObject): Unit = send { completion ->
        ios.sendRealtimeInput(input.toString(), completion)
    }

    actual suspend fun sendToolResponse(functionResponses: JsonArray): Unit = send { completion ->
        ios.sendToolResponse(functionResponses.toString(), completion)
    }

    actual suspend fun close(): Unit = suspendCancellableCoroutine { continuation ->
        ios.close { continuation.resume(Unit) }
    }
}

/** The timeout of the SDK's `RequestOptions`, in seconds. */
private val NativeRequestOptions.seconds: Double get() = timeout.inWholeMilliseconds / 1000.0

/** Runs a call of the wrapper that completes with the JSON text of a response; cancelling the coroutine cancels it. */
private suspend fun call(start: (completion: (String?, NSError?) -> Unit) -> FAICancellable): JsonObject = suspendCancellableCoroutine { continuation ->
    val cancellable = start { json, error ->
        continuation.resumeWith(runCatching { if (error != null) throw error.toNativeAIError() else json.toJsonObject() })
    }
    continuation.invokeOnCancellation { cancellable.cancel() }
}

/**
 * Runs a streaming call of the wrapper as the flow of the JSON objects of its chunks; cancelling the collection cancels
 * the call. The wrapper's callbacks cannot wait for the collector, so the buffer is unlimited.
 */
private fun stream(start: (onChunk: (String?) -> Unit, completion: (NSError?) -> Unit) -> FAICancellable): Flow<JsonObject> = callbackFlow {
    val cancellable = start(
        { chunk -> runCatching { chunk.toJsonObject() }.fold(onSuccess = { trySend(it) }, onFailure = { close(it) }) },
        { error -> close(error?.toNativeAIError()) },
    )
    awaitClose { cancellable.cancel() }
}.buffer(Channel.UNLIMITED)

/** Runs a send of the wrapper's Live API session, which completes with nil or an error. */
private suspend fun send(start: (completion: (NSError?) -> Unit) -> Unit): Unit = suspendCancellableCoroutine { continuation ->
    start { error ->
        continuation.resumeWith(if (error != null) Result.failure(error.toNativeAIError()) else Result.success(Unit))
    }
}

/** The JSON object of the wrapper's JSON text; anything else fails with [NativeAIError.Kind.SERIALIZATION]. */
private fun String?.toJsonObject(): JsonObject {
    val element = try {
        this?.let { Json.parseToJsonElement(it) }
    } catch (e: Exception) {
        throw NativeAIError(NativeAIError.Kind.SERIALIZATION, "The response of the iOS SDK could not be parsed: ${e.message}", cause = e)
    }
    return element as? JsonObject ?: throw NativeAIError(NativeAIError.Kind.SERIALIZATION, "The iOS SDK returned no JSON object: $this")
}

/**
 * The [NativeAIError] of an error of the wrapper: its code is the ordinal of the kind, and a server error carries the
 * HTTP status and the JSON text of the error's details in its `userInfo`.
 */
private fun NSError?.toNativeAIError(): NativeAIError {
    if (this == null) return NativeAIError(NativeAIError.Kind.UNKNOWN, "The iOS SDK completed without a result.")
    return NativeAIError(
        kind = NativeAIError.Kind.entries.getOrElse(code.toInt()) { NativeAIError.Kind.UNKNOWN },
        message = localizedDescription,
        status = when (val status = userInfo[STATUS_KEY]) {
            is NSNumber -> status.intValue
            is Number -> status.toInt()
            else -> null
        },
        details = (userInfo[DETAILS_KEY] as? String)
            ?.let { text -> runCatching { Json.parseToJsonElement(text) as? JsonArray }.getOrNull() }
            ?.filterIsInstance<JsonObject>()
            .orEmpty(),
    )
}
