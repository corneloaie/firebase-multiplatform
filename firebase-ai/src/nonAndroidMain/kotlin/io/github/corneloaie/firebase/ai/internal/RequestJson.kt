/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 *
 * Adapted in part from the Firebase AI Logic Android SDK (firebase-android-sdk/ai-logic/firebase-ai, Copyright Google
 * LLC, Apache License 2.0), whose serialization and error handling it follows.
 */

package io.github.corneloaie.firebase.ai.internal

import com.google.firebase.ai.type.ActivityDetectionConfig
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.AutoFunctionDeclaration
import com.google.firebase.ai.type.CodeExecutionResultPart
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ContextWindowCompressionConfig
import com.google.firebase.ai.type.ExecutableCodePart
import com.google.firebase.ai.type.FileDataPart
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionCallingConfig
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerationConfig
import com.google.firebase.ai.type.HarmBlockMethod
import com.google.firebase.ai.type.HarmBlockThreshold
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.InvalidStateException
import com.google.firebase.ai.type.JsonSchema
import com.google.firebase.ai.type.LiveGenerationConfig
import com.google.firebase.ai.type.MultiSpeakerVoiceConfig
import com.google.firebase.ai.type.Part
import com.google.firebase.ai.type.RealtimeInputConfig
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.RetrievalConfig
import com.google.firebase.ai.type.SafetySetting
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SerializationException
import com.google.firebase.ai.type.SessionResumptionConfig
import com.google.firebase.ai.type.SpeakerVoiceConfig
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.TemplateAutoFunctionDeclaration
import com.google.firebase.ai.type.TemplateFunctionDeclaration
import com.google.firebase.ai.type.TemplateTool
import com.google.firebase.ai.type.TemplateToolConfig
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.ThinkingConfig
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.ToolConfig
import com.google.firebase.ai.type.Voice
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64

/*
 * The public request types as the JSON of the Firebase AI Logic REST API (the requests the Android SDK serializes with
 * its internal classes; the field names and enum values are the same). The platform SDKs take these requests: the JS
 * SDK as JavaScript objects, the iOS SDK through the Objective-C wrapper, which parses them into its Swift types.
 */

internal fun JsonObjectBuilder.putIfNotNull(key: String, value: JsonElement?) {
    if (value != null) put(key, value)
}

internal fun JsonObjectBuilder.putIfNotNull(key: String, value: String?) {
    if (value != null) put(key, value)
}

internal fun JsonObjectBuilder.putIfNotNull(key: String, value: Number?) {
    if (value != null) put(key, value)
}

internal fun JsonObjectBuilder.putIfNotNull(key: String, value: Boolean?) {
    if (value != null) put(key, value)
}

internal fun List<String>.toJsonArray(): JsonArray = JsonArray(map { JsonPrimitive(it) })

/** The `generateContent` (and `countTokens`) request of the REST API, without its `model`. */
internal fun generateContentRequest(
    contents: List<Content>,
    safetySettings: List<SafetySetting>?,
    generationConfig: GenerationConfig?,
    tools: List<Tool>?,
    toolConfig: ToolConfig?,
    systemInstruction: Content?,
): JsonObject = buildJsonObject {
    put("contents", JsonArray(contents.map { it.toJson() }))
    putIfNotNull("safetySettings", safetySettings?.let { list -> JsonArray(list.map { it.toJson() }) })
    putIfNotNull("generationConfig", generationConfig?.toJson())
    putIfNotNull("tools", tools?.takeIf { it.isNotEmpty() }?.let { list -> JsonArray(list.map { it.toJson() }) })
    putIfNotNull("toolConfig", toolConfig?.toJson())
    putIfNotNull("systemInstruction", systemInstruction?.copy(role = "system")?.toJson())
}

/**
 * The `countTokens` request: the contents with the tools, system instruction and generation configuration that count
 * towards the prompt (the Agent Platform backend's `countTokens` takes these fields only; the SDKs wrap them as the
 * Google Developer API needs).
 */
internal fun countTokensRequest(
    contents: List<Content>,
    generationConfig: GenerationConfig?,
    tools: List<Tool>?,
    systemInstruction: Content?,
): JsonObject = buildJsonObject {
    put("contents", JsonArray(contents.map { it.toJson() }))
    putIfNotNull("tools", tools?.takeIf { it.isNotEmpty() }?.let { list -> JsonArray(list.map { it.toJson() }) })
    putIfNotNull("systemInstruction", systemInstruction?.copy(role = "system")?.toJson())
    putIfNotNull("generationConfig", generationConfig?.toJson())
}

/** The request of a server prompt template: its inputs, the conversation so far, and the template's tools. */
internal fun templateGenerateContentRequest(
    inputs: Map<String, Any>,
    history: List<Content>?,
    tools: List<TemplateTool>?,
    toolConfig: TemplateToolConfig?,
): JsonObject = buildJsonObject {
    put("inputs", inputs.toJsonElement())
    putIfNotNull("history", history?.let { list -> JsonArray(list.map { it.toJson(ignoreThoughtFlag = true) }) })
    putIfNotNull("tools", tools?.let { list -> JsonArray(list.map { it.toJson() }) })
    putIfNotNull(
        "toolConfig",
        toolConfig?.retrievalConfig?.let { retrieval -> buildJsonObject { put("retrievalConfig", retrieval.toJson()) } },
    )
}

/** A template input (a map of strings, numbers, booleans, lists and maps, as the Android SDK's `JSONObject(inputs)`). */
internal fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Char -> JsonPrimitive(toString())
    is Map<*, *> -> JsonObject(entries.associate { (key, value) -> key.toString() to value.toJsonElement() })
    is Iterable<*> -> JsonArray(map { it.toJsonElement() })
    is Array<*> -> JsonArray(map { it.toJsonElement() })
    is IntArray -> JsonArray(map { JsonPrimitive(it) })
    is LongArray -> JsonArray(map { JsonPrimitive(it) })
    is DoubleArray -> JsonArray(map { JsonPrimitive(it) })
    is FloatArray -> JsonArray(map { JsonPrimitive(it) })
    is BooleanArray -> JsonArray(map { JsonPrimitive(it) })
    else -> JsonPrimitive(toString())
}

internal fun Content.toJson(ignoreThoughtFlag: Boolean = false): JsonObject = buildJsonObject {
    put("role", role ?: "user")
    put("parts", JsonArray(parts.map { it.toJson(ignoreThoughtFlag) }))
}

internal fun ByteArray.toBase64(): String = Base64.encode(this)

internal fun InlineData.toJson(): JsonObject = buildJsonObject {
    put("mimeType", mimeType)
    put("data", data.toBase64())
    putIfNotNull("displayName", displayName)
}

/** A part, with the `thought` flag unless [ignoreThoughtFlag] (the parts of a function response and of a template's history). */
internal fun Part.toJson(ignoreThoughtFlag: Boolean = false): JsonObject {
    val thought = if (ignoreThoughtFlag) null else isThought
    fun JsonObjectBuilder.thinking(thoughtSignature: String?) {
        putIfNotNull("thought", thought)
        putIfNotNull("thoughtSignature", thoughtSignature)
    }
    return when (this) {
        is TextPart -> buildJsonObject {
            put("text", text)
            thinking(thoughtSignature)
            speechMetadata?.let { metadata ->
                put(
                    "speechMetadata",
                    buildJsonObject {
                        putIfNotNull("speaker", metadata.speaker)
                        putIfNotNull("style", metadata.style)
                    },
                )
            }
        }

        is InlineDataPart -> buildJsonObject {
            put(
                "inlineData",
                buildJsonObject {
                    put("mimeType", mimeType)
                    put("data", inlineData.toBase64())
                    putIfNotNull("displayName", displayName)
                },
            )
            thinking(thoughtSignature)
        }

        is FunctionCallPart -> buildJsonObject {
            put(
                "functionCall",
                buildJsonObject {
                    put("name", name)
                    put("args", JsonObject(args))
                    putIfNotNull("id", id)
                },
            )
            thinking(thoughtSignature)
        }

        is FunctionResponsePart -> buildJsonObject {
            put("functionResponse", functionResponseJson())
            thinking(thoughtSignature)
        }

        is FileDataPart -> buildJsonObject {
            put(
                "fileData",
                buildJsonObject {
                    put("mimeType", mimeType)
                    put("fileUri", uri)
                },
            )
            thinking(thoughtSignature)
        }

        is ExecutableCodePart -> buildJsonObject {
            put(
                "executableCode",
                buildJsonObject {
                    put("language", language)
                    put("code", code)
                },
            )
            thinking(thoughtSignature)
        }

        is CodeExecutionResultPart -> buildJsonObject {
            put(
                "codeExecutionResult",
                buildJsonObject {
                    put("outcome", outcome)
                    put("output", output)
                },
            )
            thinking(thoughtSignature)
        }

        is UnknownPart -> buildJsonObject { thinking(thoughtSignature) }

        else -> throw SerializationException("The given subclass of Part (${this::class.simpleName}) is not supported in the serialization yet.")
    }
}

/** The `functionResponse` of a [FunctionResponsePart] (also what the Live API's tool response carries). */
internal fun FunctionResponsePart.functionResponseJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("response", response)
    putIfNotNull("id", id)
    if (parts.isNotEmpty()) put("parts", JsonArray(parts.map { it.toJson(ignoreThoughtFlag = true) }))
}

internal fun HarmCategory.toJson(): String = when (this) {
    HarmCategory.HARASSMENT -> "HARM_CATEGORY_HARASSMENT"
    HarmCategory.HATE_SPEECH -> "HARM_CATEGORY_HATE_SPEECH"
    HarmCategory.SEXUALLY_EXPLICIT -> "HARM_CATEGORY_SEXUALLY_EXPLICIT"
    HarmCategory.DANGEROUS_CONTENT -> "HARM_CATEGORY_DANGEROUS_CONTENT"
    HarmCategory.CIVIC_INTEGRITY -> "HARM_CATEGORY_CIVIC_INTEGRITY"
    HarmCategory.IMAGE_HATE -> "HARM_CATEGORY_IMAGE_HATE"
    HarmCategory.IMAGE_DANGEROUS_CONTENT -> "HARM_CATEGORY_IMAGE_DANGEROUS_CONTENT"
    HarmCategory.IMAGE_HARASSMENT -> "HARM_CATEGORY_IMAGE_HARASSMENT"
    HarmCategory.IMAGE_SEXUALLY_EXPLICIT -> "HARM_CATEGORY_IMAGE_SEXUALLY_EXPLICIT"
    HarmCategory.UNKNOWN -> "UNKNOWN"
    else -> throw missingCase("HarmCategory", ordinal)
}

internal fun HarmBlockThreshold.toJson(): String = when (this) {
    HarmBlockThreshold.OFF -> "OFF"
    HarmBlockThreshold.NONE -> "BLOCK_NONE"
    HarmBlockThreshold.ONLY_HIGH -> "BLOCK_ONLY_HIGH"
    HarmBlockThreshold.MEDIUM_AND_ABOVE -> "BLOCK_MEDIUM_AND_ABOVE"
    HarmBlockThreshold.LOW_AND_ABOVE -> "BLOCK_LOW_AND_ABOVE"
    else -> throw missingCase("HarmBlockThreshold", ordinal)
}

internal fun HarmBlockMethod.toJson(): String = when (this) {
    HarmBlockMethod.SEVERITY -> "SEVERITY"
    HarmBlockMethod.PROBABILITY -> "PROBABILITY"
    else -> throw missingCase("HarmBlockMethod", ordinal)
}

internal fun SafetySetting.toJson(): JsonObject = buildJsonObject {
    put("category", harmCategory.toJson())
    put("threshold", threshold.toJson())
    putIfNotNull("method", method?.toJson())
}

internal fun ResponseModality.toJson(): String = when (this) {
    ResponseModality.TEXT -> "TEXT"
    ResponseModality.IMAGE -> "IMAGE"
    else -> "AUDIO"
}

internal fun ThinkingLevel.toJson(): String = when (this) {
    ThinkingLevel.MINIMAL -> "MINIMAL"
    ThinkingLevel.LOW -> "LOW"
    ThinkingLevel.MEDIUM -> "MEDIUM"
    ThinkingLevel.HIGH -> "HIGH"
    else -> throw missingCase("ThinkingLevel", ordinal)
}

internal fun ThinkingConfig.toJson(): JsonObject = buildJsonObject {
    putIfNotNull("thinkingBudget", thinkingBudget)
    putIfNotNull("includeThoughts", includeThoughts)
    putIfNotNull("thinkingLevel", thinkingLevel?.toJson())
}

internal fun ImageConfig.toJson(): JsonObject = buildJsonObject {
    putIfNotNull("aspectRatio", aspectRatio?.value)
    putIfNotNull("imageSize", imageSize?.value)
}

private fun Voice.toJson(): JsonObject = buildJsonObject {
    put("prebuiltVoiceConfig", buildJsonObject { put("voiceName", voiceName) })
}

private fun SpeakerVoiceConfig.toJson(): JsonObject = buildJsonObject {
    put("speaker", speaker)
    put("voiceConfig", voice.toJson())
}

private fun MultiSpeakerVoiceConfig.toJson(): JsonObject = buildJsonObject {
    put("speakerVoiceConfigs", JsonArray(speakerVoiceConfigs.map { it.toJson() }))
}

internal fun SpeechConfig.toJson(): JsonObject = buildJsonObject {
    putIfNotNull("voiceConfig", voice?.toJson())
    putIfNotNull("multiSpeakerVoiceConfig", multiSpeakerVoiceConfig?.toJson())
    putIfNotNull("languageCode", languageCode)
}

internal fun GenerationConfig.toJson(): JsonObject = buildJsonObject {
    putIfNotNull("temperature", temperature)
    putIfNotNull("topP", topP)
    putIfNotNull("topK", topK)
    putIfNotNull("candidateCount", candidateCount)
    putIfNotNull("maxOutputTokens", maxOutputTokens)
    putIfNotNull("stopSequences", stopSequences?.toJsonArray())
    putIfNotNull("responseMimeType", responseMimeType)
    putIfNotNull("presencePenalty", presencePenalty)
    putIfNotNull("frequencyPenalty", frequencyPenalty)
    putIfNotNull("responseSchema", responseSchema?.toOpenApiJson())
    putIfNotNull("responseJsonSchema", responseJsonSchema?.toJsonSchemaJson())
    putIfNotNull("responseModalities", responseModalities?.map { it.toJson() }?.toJsonArray())
    putIfNotNull("thinkingConfig", thinkingConfig?.toJson())
    putIfNotNull("imageConfig", imageConfig?.toJson())
    putIfNotNull("speechConfig", speechConfig?.toJson())
}

/** A [Schema] in the OpenAPI form of the REST API's `responseSchema` and function `parameters`. */
internal fun Schema.toOpenApiJson(): JsonObject = buildJsonObject {
    putIfNotNull("type", type.takeIf { it != "ANYOF" })
    putIfNotNull("description", description)
    putIfNotNull("format", format)
    putIfNotNull("nullable", nullable)
    putIfNotNull("enum", enum?.toJsonArray())
    putIfNotNull("properties", properties?.let { map -> JsonObject(map.mapValues { it.value.toOpenApiJson() }) })
    putIfNotNull("required", required?.toJsonArray())
    putIfNotNull("items", items?.toOpenApiJson())
    putIfNotNull("title", title)
    putIfNotNull("minItems", minItems)
    putIfNotNull("maxItems", maxItems)
    putIfNotNull("minimum", minimum)
    putIfNotNull("maximum", maximum)
    putIfNotNull("anyOf", anyOf?.let { list -> JsonArray(list.map { it.toOpenApiJson() }) })
}

/** The JSON Schema form of [Schema] (`responseJsonSchema`), as the Android SDK's `toInternalJson`. */
internal fun Schema.toJsonSchemaJson(): JsonObject = jsonSchemaJson(
    type = type,
    description = description,
    format = format,
    nullable = nullable,
    enum = enum,
    properties = properties?.mapValues { it.value.toJsonSchemaJson() },
    required = required,
    items = items?.toJsonSchemaJson(),
    title = title,
    minItems = minItems,
    maxItems = maxItems,
    minimum = minimum,
    maximum = maximum,
    anyOf = anyOf?.map { it.toJsonSchemaJson() },
)

/** A [JsonSchema] as the JSON Schema of the REST API (`responseJsonSchema`, `parametersJsonSchema`). */
internal fun JsonSchema<*>.toJsonSchemaJson(): JsonObject = jsonSchemaJson(
    type = type,
    description = description,
    format = format,
    nullable = nullable,
    enum = enum,
    properties = properties?.mapValues { it.value.toJsonSchemaJson() },
    required = required,
    items = items?.toJsonSchemaJson(),
    title = title,
    minItems = minItems,
    maxItems = maxItems,
    minimum = minimum,
    maximum = maximum,
    anyOf = anyOf?.map { it.toJsonSchemaJson() },
)

private fun jsonSchemaJson(
    type: String,
    description: String?,
    format: String?,
    nullable: Boolean?,
    enum: List<String>?,
    properties: Map<String, JsonObject>?,
    required: List<String>?,
    items: JsonObject?,
    title: String?,
    minItems: Int?,
    maxItems: Int?,
    minimum: Double?,
    maximum: Double?,
    anyOf: List<JsonObject>?,
): JsonObject {
    val outType = if (type == "ANYOF" || (type == "STRING" && format == "enum")) null else type.lowercase()
    val (outMinimum, outMaximum) = if (outType == "integer" && format == "int32") {
        (minimum ?: Int.MIN_VALUE.toDouble()) to (maximum ?: Int.MAX_VALUE.toDouble())
    } else {
        minimum to maximum
    }
    val outFormat = if ((outType == "integer" && format == "int32") || (outType == "number" && format == "float") || format == "enum") null else format
    val isNullable = nullable == true
    return buildJsonObject {
        putIfNotNull("type", outType?.let { if (isNullable) listOf(it, "null").toJsonArray() else JsonPrimitive(it) })
        putIfNotNull("description", description)
        putIfNotNull("format", outFormat)
        putIfNotNull("enum", enum?.let { if (isNullable) JsonArray(it.map(::JsonPrimitive) + JsonPrimitive("null")) else it.toJsonArray() })
        putIfNotNull("properties", properties?.let(::JsonObject))
        putIfNotNull("required", required?.toJsonArray())
        putIfNotNull("items", items)
        putIfNotNull("title", title)
        putIfNotNull("minItems", minItems)
        putIfNotNull("maxItems", maxItems)
        putIfNotNull("minimum", outMinimum)
        putIfNotNull("maximum", outMaximum)
        putIfNotNull("anyOf", anyOf?.let(::JsonArray))
    }
}

internal fun FunctionDeclaration.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description)
    put("parameters", schema.toOpenApiJson())
}

internal fun AutoFunctionDeclaration<*, *>.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description)
    put("parametersJsonSchema", inputSchema.toJsonSchemaJson())
    putIfNotNull("responseJsonSchema", outputSchema?.toJsonSchemaJson())
}

internal fun Tool.toJson(): JsonObject = buildJsonObject {
    val declarations = functionDeclarations.orEmpty().map { it.toJson() } + autoFunctionDeclarations.orEmpty().map { it.toJson() }
    // The Android SDK always sends the list, empty for the other tools.
    put("functionDeclarations", JsonArray(declarations))
    if (googleSearch != null) put("googleSearch", JsonObject(emptyMap()))
    putIfNotNull("codeExecution", codeExecution)
    if (urlContext != null) put("urlContext", JsonObject(emptyMap()))
    if (googleMaps != null) put("googleMaps", JsonObject(emptyMap()))
}

internal fun RetrievalConfig.toJson(): JsonObject = buildJsonObject {
    putIfNotNull(
        "latLng",
        latLng?.let {
            buildJsonObject {
                put("latitude", it.latitude)
                put("longitude", it.longitude)
            }
        },
    )
    putIfNotNull("languageCode", languageCode)
}

internal fun ToolConfig.toJson(): JsonObject = buildJsonObject {
    putIfNotNull(
        "functionCallingConfig",
        functionCallingConfig?.let {
            buildJsonObject {
                put(
                    "mode",
                    when (it.mode) {
                        FunctionCallingConfig.Mode.ANY -> "ANY"
                        FunctionCallingConfig.Mode.AUTO -> "AUTO"
                        FunctionCallingConfig.Mode.NONE -> "NONE"
                    },
                )
                putIfNotNull("allowedFunctionNames", it.allowedFunctionNames?.toJsonArray())
            }
        },
    )
    putIfNotNull("retrievalConfig", retrievalConfig?.toJson())
}

private fun TemplateFunctionDeclaration.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    putIfNotNull("inputSchema", inputSchema?.toJsonSchemaJson())
    putIfNotNull("outputSchema", outputSchema?.toJsonSchemaJson())
}

private fun TemplateAutoFunctionDeclaration<*, *>.toJson(): JsonObject = buildJsonObject {
    put("name", name)
    put("inputSchema", inputSchema.toJsonSchemaJson())
    putIfNotNull("outputSchema", outputSchema?.toJsonSchemaJson())
}

internal fun TemplateTool.toJson(): JsonObject = buildJsonObject {
    val declarations = functionDeclarations.orEmpty().map { it.toJson() } + autoFunctionDeclarations.orEmpty().map { it.toJson() }
    // The Android SDK always sends the list, empty for a Google Maps tool.
    put("templateFunctions", JsonArray(declarations))
    if (googleMaps != null) put("googleMaps", JsonObject(emptyMap()))
}

/* The Live API (see LiveSession.nonAndroid.kt). */

internal fun LiveGenerationConfig.toJson(): JsonObject = buildJsonObject {
    putIfNotNull("temperature", temperature)
    putIfNotNull("topP", topP)
    putIfNotNull("topK", topK)
    putIfNotNull("maxOutputTokens", maxOutputTokens)
    putIfNotNull("presencePenalty", presencePenalty)
    putIfNotNull("frequencyPenalty", frequencyPenalty)
    putIfNotNull("speechConfig", speechConfig?.toJson())
    putIfNotNull("responseModalities", responseModality?.let { listOf(it.toJson()).toJsonArray() })
}

private fun ActivityDetectionConfig.toJson(): JsonObject = buildJsonObject {
    putIfNotNull("startOfSpeechSensitivity", startSensitivity?.let { "START_SENSITIVITY_${it.value}" })
    putIfNotNull("endOfSpeechSensitivity", endSensitivity?.let { "END_SENSITIVITY_${it.value}" })
    putIfNotNull("prefixPaddingMs", prefixPaddingMs)
    putIfNotNull("silenceDurationMs", silenceDurationMs)
    putIfNotNull("disabled", disabled)
}

internal fun RealtimeInputConfig.toJson(): JsonObject = buildJsonObject {
    putIfNotNull("automaticActivityDetection", automaticActivityDetection?.toJson())
    putIfNotNull("activityHandling", activityHandling?.value)
    putIfNotNull("turnCoverage", turnCoverage?.value)
}

internal fun ContextWindowCompressionConfig.toJson(): JsonObject = buildJsonObject {
    putIfNotNull("triggerTokens", triggerTokens)
    putIfNotNull("slidingWindow", slidingWindow?.let { buildJsonObject { putIfNotNull("targetTokens", it.targetTokens) } })
}

internal fun SessionResumptionConfig.toJson(): JsonObject = buildJsonObject { putIfNotNull("handle", handle) }

@Suppress("UnusedReceiverParameter")
private fun AudioTranscriptionConfig.toJson(): JsonObject = JsonObject(emptyMap())

/**
 * The `setup` of a Live API session (the first message the Android SDK sends over the WebSocket), without its `model`.
 * The platform SDKs open the session from it.
 */
internal fun liveSetup(
    config: LiveGenerationConfig?,
    tools: List<Tool>,
    systemInstruction: Content?,
    sessionResumption: SessionResumptionConfig?,
): JsonObject = buildJsonObject {
    putIfNotNull("generationConfig", config?.toJson())
    putIfNotNull("tools", tools.takeIf { it.isNotEmpty() }?.let { list -> JsonArray(list.map { it.toJson() }) })
    putIfNotNull("systemInstruction", systemInstruction?.toJson())
    putIfNotNull("inputAudioTranscription", config?.inputAudioTranscription?.toJson())
    putIfNotNull("outputAudioTranscription", config?.outputAudioTranscription?.toJson())
    putIfNotNull("sessionResumption", sessionResumption?.toJson())
    putIfNotNull("contextWindowCompression", config?.contextWindowCompression?.toJson())
    putIfNotNull("realtimeInputConfig", config?.realtimeInputConfig?.toJson())
}

internal fun missingCase(source: String, ordinal: Int): SerializationException = SerializationException(
    "Missing case for a $source: $ordinal\nThis error indicates that one of the conversions to the request JSON needs updating.",
)

/** The Google Developer API has no `method` for safety settings (the Android SDK checks it the same way). */
internal fun checkSafetySettingsFor(googleAI: Boolean, safetySettings: List<SafetySetting>?) {
    if (googleAI && safetySettings?.any { it.method != null } == true) {
        throw InvalidStateException("HarmBlockMethod is unsupported by the Google Developer API")
    }
}

/** A realtime input of the Live API (`realtimeInput`): audio, a video frame, text or an activity signal. */
internal fun realtimeInput(
    audio: InlineData? = null,
    video: InlineData? = null,
    text: String? = null,
    activityStart: Boolean = false,
    activityEnd: Boolean = false,
): JsonObject = buildJsonObject {
    putIfNotNull("audio", audio?.toJson())
    putIfNotNull("video", video?.toJson())
    putIfNotNull("text", text)
    if (activityStart) put("activityStart", JsonObject(emptyMap()))
    if (activityEnd) put("activityEnd", JsonObject(emptyMap()))
}

/** The function responses of the Live API's `toolResponse`. */
internal fun toolResponse(functionList: List<FunctionResponsePart>): JsonArray = buildJsonArray {
    functionList.forEach { add(it.functionResponseJson()) }
}
