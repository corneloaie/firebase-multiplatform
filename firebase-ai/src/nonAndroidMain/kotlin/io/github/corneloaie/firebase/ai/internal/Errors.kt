/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 *
 * Adapted in part from the Firebase AI Logic Android SDK (firebase-android-sdk/ai-logic/firebase-ai, Copyright Google
 * LLC, Apache License 2.0), whose serialization and error handling it follows.
 */

package io.github.corneloaie.firebase.ai.internal

import com.google.firebase.ai.type.APINotConfiguredException
import com.google.firebase.ai.type.FinishReason
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.InvalidStateException
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.SerializationException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.ServiceConnectionHandshakeFailedException
import com.google.firebase.ai.type.ServiceDisabledException
import com.google.firebase.ai.type.UnknownException
import com.google.firebase.ai.type.UnsupportedUserLocationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.json.JsonObject

/**
 * A failure the platform SDK reported, as the native layer hands it over: the kind of failure, and for a request the
 * server answered with an error, the HTTP [status] and the gRPC error's [message] and [details] (the `details` of the
 * REST API's error, of which `reason`, `domain` and `metadata` are read).
 */
internal class NativeAIError(
    val kind: Kind,
    message: String,
    val status: Int? = null,
    val details: List<JsonObject> = emptyList(),
    cause: Throwable? = null,
) : Exception(message, cause) {
    internal enum class Kind {
        /** The server answered with an error (see [status], [message] and [details]). */
        SERVER,

        /** The request did not complete in time. */
        TIMEOUT,

        /** The response could not be parsed. */
        SERIALIZATION,

        /** The request was invalid before it was sent (the SDK rejected it). */
        INVALID_REQUEST,

        /** The Live API session could not be opened. */
        HANDSHAKE,

        /** Anything else. */
        UNKNOWN,
    }
}

/**
 * The [FirebaseAIException] of [cause], as the Android SDK maps the errors of its HTTP client (`FirebaseAIException.from`
 * and `validateResponse`): server errors are told apart by their message and details.
 */
internal fun firebaseAIException(cause: Throwable): FirebaseAIException = when (cause) {
    is FirebaseAIException -> cause

    is TimeoutCancellationException -> RequestTimeoutException("The request failed to complete in the allotted time.", cause)

    is CancellationException -> throw cause

    is NativeAIError -> when (cause.kind) {
        NativeAIError.Kind.SERVER -> serverException(cause)
        NativeAIError.Kind.TIMEOUT -> RequestTimeoutException("The request failed to complete in the allotted time.", cause)
        NativeAIError.Kind.SERIALIZATION -> SerializationException(cause.message ?: "Something went wrong while trying to deserialize a response from the server.", cause)
        NativeAIError.Kind.INVALID_REQUEST -> InvalidStateException(cause.message ?: "The request is invalid.", cause)
        NativeAIError.Kind.HANDSHAKE -> ServiceConnectionHandshakeFailedException(cause.message ?: "Unable to connect to the server", cause)
        NativeAIError.Kind.UNKNOWN -> UnknownException(cause.message ?: "Something unexpected happened.", cause)
    }

    is kotlinx.serialization.SerializationException -> SerializationException(cause.message ?: "Something went wrong while trying to deserialize a response from the server.", cause)

    else -> UnknownException("Something unexpected happened.", cause)
}

private fun serverException(error: NativeAIError): FirebaseAIException {
    val message = error.message.orEmpty()
    return when {
        message.contains("API key not valid") -> InvalidAPIKeyException(message, error)

        message.contains("User location is not supported for the API use") -> UnsupportedUserLocationException(error)

        message.contains("quota") -> QuotaExceededException(message, error)

        message.contains("The prompt could not be submitted") -> PromptBlockedException(message, error)

        message.contains("genai config not found") -> APINotConfiguredException(
            "The Gemini Developer API is not enabled, to enable and configure, see https://firebase.google.com/docs/ai-logic/faq-and-troubleshooting#error-genai-config-not-found",
            error,
        )

        error.details.any { it.string("reason") == "SERVICE_DISABLED" && it.string("domain") == "googleapis.com" } -> {
            val detail = error.details.first { it.string("reason") == "SERVICE_DISABLED" }
            ServiceDisabledException(
                if (detail.obj("metadata")?.string("service") == "firebasevertexai.googleapis.com") {
                    "The Firebase AI SDK requires the Vertex AI in Firebase API (`firebasevertexai.googleapis.com`) to be enabled in " +
                        "your Firebase project. Enable this API by visiting the Firebase Console at " +
                        "https://console.firebase.google.com/project/_/ailogic and clicking \"Get started\". If you enabled this API " +
                        "recently, wait a few minutes for the action to propagate to our systems and then retry."
                } else {
                    message
                },
                error,
            )
        }

        error.status == 404 && message.contains("<html", ignoreCase = true) -> ServerException(
            "URL not found. Please verify the location used to create the `FirebaseAI` object. See " +
                "https://firebase.google.com/docs/ai-logic/locations?api=vertex#available-locations for the list of available " +
                "locations. Raw response: $message",
            error,
        )

        else -> ServerException(message, error)
    }
}

/** Runs [block], turning what it throws into a [FirebaseAIException] (a cancellation is rethrown as it is). */
internal suspend fun <T> catchAsync(block: suspend () -> T): T = try {
    block()
} catch (e: Throwable) {
    throw firebaseAIException(e)
}

/**
 * Checks a response as the Android SDK does: a response without candidates and prompt feedback cannot be decoded, a
 * blocked prompt throws [PromptBlockedException] and a candidate that did not finish with `STOP` throws
 * [ResponseStoppedException].
 */
internal fun GenerateContentResponse.validate(): GenerateContentResponse = apply {
    if (candidates.isEmpty() && promptFeedback == null) {
        throw SerializationException("Error deserializing response, found no valid fields")
    }
    promptFeedback?.blockReason?.let { throw PromptBlockedException(this) }
    candidates.mapNotNull { it.finishReason }.firstOrNull { it != FinishReason.STOP }?.let { throw ResponseStoppedException(this) }
}
