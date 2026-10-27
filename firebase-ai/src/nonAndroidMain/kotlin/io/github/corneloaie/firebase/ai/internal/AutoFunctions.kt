/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 *
 * Adapted in part from the Firebase AI Logic Android SDK (firebase-android-sdk/ai-logic/firebase-ai, Copyright Google
 * LLC, Apache License 2.0), whose serialization and error handling it follows.
 */

package io.github.corneloaie.firebase.ai.internal

import com.google.firebase.ai.type.FirebaseAutoFunctionException
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.JsonSchema
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Runs the automatic function [name] for [call], as the Android SDK's models do: the call's arguments are decoded with
 * [inputSchema]'s serializer, the result encoded with [outputSchema]'s (a function without an output schema returns a
 * [FunctionResponsePart] itself), and a [FirebaseAutoFunctionException] is sent back to the model as `{"error": ...}`.
 */
@Suppress("UNCHECKED_CAST")
internal suspend fun executeAutoFunction(
    call: FunctionCallPart,
    name: String,
    inputSchema: JsonSchema<*>,
    outputSchema: JsonSchema<*>?,
    functionReference: Any?,
): FunctionResponsePart {
    val input = AIJson.decodeFromString(inputSchema.getSerializer(), JsonObject(call.args).toString())
    val reference = functionReference as? (suspend (Any) -> Any) ?: throw RuntimeException("Function reference for $name is missing")
    return try {
        val output = reference(input)
        val serializer = outputSchema?.getSerializer() as KSerializer<Any>?
        if (serializer != null) {
            FunctionResponsePart.from(AIJson.encodeToJsonElement(serializer, output).jsonObject).normalizeAgainstCall(call)
        } else {
            (output as FunctionResponsePart).normalizeAgainstCall(call)
        }
    } catch (e: FirebaseAutoFunctionException) {
        FunctionResponsePart.from(JsonObject(mapOf("error" to JsonPrimitive(e.message)))).normalizeAgainstCall(call)
    }
}
