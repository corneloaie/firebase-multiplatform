/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.ai.internal

import com.google.firebase.ai.InferenceSource
import com.google.firebase.ai.type.BlockReason
import com.google.firebase.ai.type.Candidate
import com.google.firebase.ai.type.Citation
import com.google.firebase.ai.type.CitationMetadata
import com.google.firebase.ai.type.CodeExecutionResultPart
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ContentModality
import com.google.firebase.ai.type.CountTokensResponse
import com.google.firebase.ai.type.ExecutableCodePart
import com.google.firebase.ai.type.FileDataPart
import com.google.firebase.ai.type.FinishReason
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GoogleMapsGroundingChunk
import com.google.firebase.ai.type.GroundingChunk
import com.google.firebase.ai.type.GroundingMetadata
import com.google.firebase.ai.type.GroundingSupport
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.HarmProbability
import com.google.firebase.ai.type.HarmSeverity
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.LiveServerContent
import com.google.firebase.ai.type.LiveServerGoAway
import com.google.firebase.ai.type.LiveServerMessage
import com.google.firebase.ai.type.LiveServerSetupComplete
import com.google.firebase.ai.type.LiveServerToolCall
import com.google.firebase.ai.type.LiveServerToolCallCancellation
import com.google.firebase.ai.type.LiveServerUnknownMessage
import com.google.firebase.ai.type.LiveSessionResumptionUpdate
import com.google.firebase.ai.type.ModalityTokenCount
import com.google.firebase.ai.type.Part
import com.google.firebase.ai.type.PromptFeedback
import com.google.firebase.ai.type.SafetyRating
import com.google.firebase.ai.type.SearchEntryPoint
import com.google.firebase.ai.type.Segment
import com.google.firebase.ai.type.SerializationException
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.Transcription
import com.google.firebase.ai.type.UrlContextMetadata
import com.google.firebase.ai.type.UrlMetadata
import com.google.firebase.ai.type.UrlRetrievalStatus
import com.google.firebase.ai.type.UsageMetadata
import com.google.firebase.ai.type.WebGroundingChunk
import com.google.firebase.ai.type.content
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlin.io.encoding.Base64
import kotlin.time.Duration.Companion.seconds

/*
 * The responses of the Firebase AI Logic REST API, as the platform SDKs hand them over (the JS SDK's response objects,
 * the Objective-C wrapper's JSON of the iOS SDK's), turned into the public types the way the Android SDK decodes the
 * same JSON: unknown values become the first ordinal (`UNKNOWN`), missing counts zero, unknown parts are dropped.
 */

/** The JSON configuration of the module's own encoding and decoding (generateObject, automatic function calls). */
internal val AIJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.array(key: String): JsonArray? = this[key] as? JsonArray

internal fun JsonObject.objects(key: String): List<JsonObject>? = array(key)?.filterIsInstance<JsonObject>()

internal fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString || it.contentOrNull != null }?.contentOrNull

internal fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() ?: it.contentOrNull?.toIntOrNull() }

internal fun JsonObject.double(key: String): Double? = (this[key] as? JsonPrimitive)?.let { it.doubleOrNull ?: it.contentOrNull?.toDoubleOrNull() }

internal fun JsonObject.boolean(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

internal fun JsonObject.strings(key: String): List<String>? = array(key)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }

internal fun String.toBytesFromBase64(): ByteArray = Base64.decode(this)

/** A part of a response the SDK does not know; it is dropped from the response's content (as the Android SDK does). */
internal class UnknownPart(override val isThought: Boolean = false, val thoughtSignature: String? = null) : Part

internal fun JsonObject.toPart(): Part {
    val thought = boolean("thought") ?: false
    val thoughtSignature = string("thoughtSignature")
    obj("functionCall")?.let { call ->
        return FunctionCallPart(
            call.string("name") ?: "",
            call.obj("args")?.mapValues { it.value } ?: emptyMap(),
            call.string("id"),
            thought,
            thoughtSignature,
        )
    }
    obj("functionResponse")?.let { response ->
        return FunctionResponsePart(
            response.string("name") ?: "",
            response.obj("response") ?: JsonObject(emptyMap()),
            response.string("id"),
            response.objects("parts")?.map { it.toPart() }.orEmpty(),
            thought,
            thoughtSignature,
        )
    }
    obj("inlineData")?.let { data ->
        return InlineDataPart(
            data.string("data")?.toBytesFromBase64() ?: ByteArray(0),
            data.string("mimeType") ?: "",
            data.string("displayName"),
            thought,
            thoughtSignature,
        )
    }
    obj("fileData")?.let { data ->
        return FileDataPart(data.string("fileUri") ?: "", data.string("mimeType") ?: "", thought, thoughtSignature)
    }
    obj("executableCode")?.let { code ->
        return ExecutableCodePart(code.string("language") ?: "", code.string("code") ?: "", thought, thoughtSignature)
    }
    obj("codeExecutionResult")?.let { result ->
        return CodeExecutionResultPart(result.string("outcome") ?: "", result.string("output") ?: "", thought, thoughtSignature)
    }
    if (containsKey("text")) return TextPart(string("text") ?: "", thought, thoughtSignature)
    return UnknownPart(thought, thoughtSignature)
}

/**
 * A content of a response. Unknown parts are dropped, and so are empty text parts; when nothing is left of the known
 * parts, a single-space text part takes their place, so that the content can be sent back in a multi-turn interaction.
 */
internal fun JsonObject.toContent(): Content {
    val role = string("role") ?: "user"
    val known = objects("parts")?.map { it.toPart() }?.filter { it !is UnknownPart }.orEmpty()
    if (known.isEmpty()) return Content(role, emptyList())
    val parts = known.filterNot { it is TextPart && it.text.isEmpty() }
    return Content(role, parts.ifEmpty { listOf(TextPart(" ")) })
}

internal fun String.toHarmCategory(): HarmCategory = when (this) {
    "HARM_CATEGORY_HARASSMENT" -> HarmCategory.HARASSMENT
    "HARM_CATEGORY_HATE_SPEECH" -> HarmCategory.HATE_SPEECH
    "HARM_CATEGORY_SEXUALLY_EXPLICIT" -> HarmCategory.SEXUALLY_EXPLICIT
    "HARM_CATEGORY_DANGEROUS_CONTENT" -> HarmCategory.DANGEROUS_CONTENT
    "HARM_CATEGORY_CIVIC_INTEGRITY" -> HarmCategory.CIVIC_INTEGRITY
    "HARM_CATEGORY_IMAGE_HATE" -> HarmCategory.IMAGE_HATE
    "HARM_CATEGORY_IMAGE_DANGEROUS_CONTENT" -> HarmCategory.IMAGE_DANGEROUS_CONTENT
    "HARM_CATEGORY_IMAGE_HARASSMENT" -> HarmCategory.IMAGE_HARASSMENT
    "HARM_CATEGORY_IMAGE_SEXUALLY_EXPLICIT" -> HarmCategory.IMAGE_SEXUALLY_EXPLICIT
    else -> HarmCategory.UNKNOWN
}

internal fun String.toHarmProbability(): HarmProbability = when (this) {
    "HIGH" -> HarmProbability.HIGH
    "MEDIUM" -> HarmProbability.MEDIUM
    "LOW" -> HarmProbability.LOW
    "NEGLIGIBLE" -> HarmProbability.NEGLIGIBLE
    else -> HarmProbability.UNKNOWN
}

internal fun String.toHarmSeverity(): HarmSeverity = when (this) {
    "HARM_SEVERITY_HIGH" -> HarmSeverity.HIGH
    "HARM_SEVERITY_MEDIUM" -> HarmSeverity.MEDIUM
    "HARM_SEVERITY_LOW" -> HarmSeverity.LOW
    "HARM_SEVERITY_NEGLIGIBLE" -> HarmSeverity.NEGLIGIBLE
    else -> HarmSeverity.UNKNOWN
}

internal fun String.toFinishReason(): FinishReason = when (this) {
    "STOP" -> FinishReason.STOP
    "MAX_TOKENS" -> FinishReason.MAX_TOKENS
    "SAFETY" -> FinishReason.SAFETY
    "RECITATION" -> FinishReason.RECITATION
    "OTHER" -> FinishReason.OTHER
    "BLOCKLIST" -> FinishReason.BLOCKLIST
    "PROHIBITED_CONTENT" -> FinishReason.PROHIBITED_CONTENT
    "SPII" -> FinishReason.SPII
    "MALFORMED_FUNCTION_CALL" -> FinishReason.MALFORMED_FUNCTION_CALL
    "IMAGE_SAFETY" -> FinishReason.IMAGE_SAFETY
    "IMAGE_PROHIBITED_CONTENT" -> FinishReason.IMAGE_PROHIBITED_CONTENT
    "IMAGE_OTHER" -> FinishReason.IMAGE_OTHER
    "NO_IMAGE" -> FinishReason.NO_IMAGE
    "IMAGE_RECITATION" -> FinishReason.IMAGE_RECITATION
    "LANGUAGE" -> FinishReason.LANGUAGE
    "UNEXPECTED_TOOL_CALL" -> FinishReason.UNEXPECTED_TOOL_CALL
    "TOO_MANY_TOOL_CALLS" -> FinishReason.TOO_MANY_TOOL_CALLS
    "MISSING_THOUGHT_SIGNATURE" -> FinishReason.MISSING_THOUGHT_SIGNATURE
    "MALFORMED_RESPONSE" -> FinishReason.MALFORMED_RESPONSE
    else -> FinishReason.UNKNOWN
}

internal fun String.toBlockReason(): BlockReason = when (this) {
    "SAFETY" -> BlockReason.SAFETY
    "OTHER" -> BlockReason.OTHER
    "BLOCKLIST" -> BlockReason.BLOCKLIST
    "PROHIBITED_CONTENT" -> BlockReason.PROHIBITED_CONTENT
    else -> BlockReason.UNKNOWN
}

internal fun String.toContentModality(): ContentModality = when (this) {
    "TEXT" -> ContentModality.TEXT
    "IMAGE" -> ContentModality.IMAGE
    "VIDEO" -> ContentModality.VIDEO
    "AUDIO" -> ContentModality.AUDIO
    "DOCUMENT" -> ContentModality.DOCUMENT
    else -> ContentModality.UNSPECIFIED
}

internal fun String.toUrlRetrievalStatus(): UrlRetrievalStatus = when (this) {
    "URL_RETRIEVAL_STATUS_SUCCESS" -> UrlRetrievalStatus.SUCCESS
    "URL_RETRIEVAL_STATUS_ERROR" -> UrlRetrievalStatus.ERROR
    "URL_RETRIEVAL_STATUS_PAYWALL" -> UrlRetrievalStatus.PAYWALL
    "URL_RETRIEVAL_STATUS_UNSAFE" -> UrlRetrievalStatus.UNSAFE
    else -> UrlRetrievalStatus.UNSPECIFIED
}

/** A safety rating, or null when the backend sends one without a category or probability (which it can). */
internal fun JsonObject.toSafetyRating(): SafetyRating? {
    val category = string("category") ?: return null
    val probability = string("probability") ?: return null
    return SafetyRating(
        category = category.toHarmCategory(),
        probability = probability.toHarmProbability(),
        probabilityScore = double("probabilityScore")?.toFloat() ?: 0f,
        blocked = boolean("blocked"),
        severity = string("severity")?.toHarmSeverity(),
        severityScore = double("severityScore")?.toFloat(),
    )
}

/**
 * The APIs for citation provide indices in UTF-8 bytes, Kotlin strings are UTF-16: walks the text of [content] to find
 * the UTF-16 index of [originalIndex] (as the Android SDK's `convertUtf8IndexToUtf16`).
 */
internal fun convertUtf8IndexToUtf16(content: Content, originalIndex: Int): Int {
    if (originalIndex == 0) return 0
    var sumIndex = 0
    var progress = 0
    for (part in content.parts) {
        val text = (part as? TextPart)?.text ?: ""
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            progress += when {
                ch.code < 0x80 -> 1
                ch.code in 0x80..0x7FF -> 2
                ch.isHighSurrogate() -> 4
                else -> 3
            }
            if (ch.isHighSurrogate() && i + 1 < text.length) i++
            i++
            if (progress >= originalIndex) return sumIndex + i
        }
        sumIndex += text.length
    }
    return originalIndex
}

private fun JsonObject.toCitation(content: Content): Citation {
    val date = obj("publicationDate")
    return Citation.create(
        title = string("title"),
        startIndex = convertUtf8IndexToUtf16(content, int("startIndex") ?: 0),
        endIndex = convertUtf8IndexToUtf16(content, int("endIndex") ?: 0),
        uri = string("uri"),
        license = string("license"),
        publicationYear = date?.int("year"),
        publicationMonth = date?.int("month"),
        publicationDay = date?.int("day"),
    )
}

/** Citation metadata: `citations` on the Agent Platform backend, `citationSources` on the Google Developer API. */
private fun JsonObject.toCitationMetadata(content: Content): CitationMetadata = CitationMetadata((objects("citations") ?: objects("citationSources")).orEmpty().map { it.toCitation(content) })

private fun JsonObject.toSegment(content: Content): Segment {
    val partIndex = int("partIndex") ?: 0
    val part = content.parts.getOrNull(partIndex)
    val partContent = Content(content.role, if (part == null) emptyList() else listOf(part))
    return Segment(
        startIndex = convertUtf8IndexToUtf16(partContent, int("startIndex") ?: 0),
        endIndex = convertUtf8IndexToUtf16(partContent, int("endIndex") ?: 0),
        partIndex = partIndex,
        text = string("text") ?: "",
    )
}

private fun JsonObject.toGroundingMetadata(content: Content): GroundingMetadata = GroundingMetadata(
    webSearchQueries = strings("webSearchQueries").orEmpty(),
    searchEntryPoint = obj("searchEntryPoint")?.let { entryPoint ->
        // Displaying a grounded result without its entry point violates the service terms (as on Android).
        val renderedContent = entryPoint.string("renderedContent") ?: throw SerializationException("renderedContent is null, should be a string")
        SearchEntryPoint(renderedContent = renderedContent, sdkBlob = entryPoint.string("sdkBlob"))
    },
    retrievalQueries = strings("retrievalQueries").orEmpty(),
    groundingChunks = objects("groundingChunks").orEmpty().map { chunk ->
        GroundingChunk(
            web = chunk.obj("web")?.let { WebGroundingChunk(uri = it.string("uri"), title = it.string("title"), domain = it.string("domain")) },
            maps = chunk.obj("maps")?.let { GoogleMapsGroundingChunk(it.string("uri"), it.string("title"), it.string("placeId")) },
        )
    },
    groundingSupports = objects("groundingSupports").orEmpty().mapNotNull { support ->
        val segment = support.obj("segment") ?: return@mapNotNull null
        GroundingSupport(
            segment = segment.toSegment(content),
            groundingChunkIndices = support.array("groundingChunkIndices")?.mapNotNull { (it as? JsonPrimitive)?.intOrNull }.orEmpty(),
        )
    },
)

private fun JsonObject.toUrlContextMetadata(): UrlContextMetadata = UrlContextMetadata(
    objects("urlMetadata").orEmpty().map { UrlMetadata(it.string("retrievedUrl"), it.string("urlRetrievalStatus")?.toUrlRetrievalStatus() ?: UrlRetrievalStatus.UNSPECIFIED) },
)

internal fun JsonObject.toCandidate(): Candidate {
    val content = obj("content")?.toContent() ?: content("model") {}
    return Candidate(
        content,
        objects("safetyRatings").orEmpty().mapNotNull { it.toSafetyRating() },
        obj("citationMetadata")?.toCitationMetadata(content),
        string("finishReason")?.toFinishReason(),
        string("finishMessage"),
        obj("groundingMetadata")?.toGroundingMetadata(content),
        obj("urlContextMetadata")?.toUrlContextMetadata(),
    )
}

internal fun JsonObject.toModalityTokenCount(): ModalityTokenCount = ModalityTokenCount.create(string("modality")?.toContentModality() ?: ContentModality.UNSPECIFIED, int("tokenCount") ?: 0)

private fun JsonObject.modalityTokenCounts(key: String): List<ModalityTokenCount> = objects(key).orEmpty().map { it.toModalityTokenCount() }

internal fun JsonObject.toUsageMetadata(): UsageMetadata = UsageMetadata(
    int("promptTokenCount") ?: 0,
    int("candidatesTokenCount") ?: 0,
    int("totalTokenCount") ?: 0,
    int("cachedContentTokenCount") ?: 0,
    promptTokensDetails = modalityTokenCounts("promptTokensDetails"),
    candidatesTokensDetails = modalityTokenCounts("candidatesTokensDetails"),
    cacheTokensDetails = modalityTokenCounts("cacheTokensDetails"),
    int("thoughtsTokenCount") ?: 0,
    int("toolUsePromptTokenCount") ?: 0,
    toolUsePromptTokensDetails = modalityTokenCounts("toolUsePromptTokensDetails"),
)

internal fun JsonObject.toPromptFeedback(): PromptFeedback = PromptFeedback(
    string("blockReason")?.toBlockReason(),
    objects("safetyRatings").orEmpty().mapNotNull { it.toSafetyRating() },
    string("blockReasonMessage"),
)

internal fun JsonObject.toGenerateContentResponse(): GenerateContentResponse = GenerateContentResponse(
    objects("candidates").orEmpty().map { it.toCandidate() },
    InferenceSource.IN_CLOUD,
    obj("promptFeedback")?.toPromptFeedback(),
    obj("usageMetadata")?.toUsageMetadata(),
    string("modelVersion"),
)

internal fun JsonObject.toCountTokensResponse(): CountTokensResponse = CountTokensResponse(int("totalTokens") ?: 0, modalityTokenCounts("promptTokensDetails"))

private fun JsonObject.toTranscription(): Transcription = Transcription(string("text"))

/**
 * A server message of the Live API, by its key: `serverContent`, `setupComplete`, `toolCall`, `toolCallCancellation`,
 * `goAway` and `sessionResumptionUpdate`; anything else is a [LiveServerUnknownMessage].
 */
internal fun JsonObject.toLiveServerMessage(): LiveServerMessage {
    obj("serverContent")?.let { content ->
        return LiveServerContent(
            content.obj("modelTurn")?.toContent(),
            content.boolean("interrupted") ?: false,
            content.boolean("turnComplete") ?: false,
            content.boolean("generationComplete") ?: false,
            content.obj("inputTranscription")?.toTranscription(),
            content.obj("outputTranscription")?.toTranscription(),
        )
    }
    if (containsKey("setupComplete")) return LiveServerSetupComplete
    obj("toolCall")?.let { toolCall ->
        return LiveServerToolCall(
            toolCall.objects("functionCalls").orEmpty().map { call ->
                FunctionCallPart(call.string("name") ?: "", call.obj("args")?.mapValues { it.value } ?: emptyMap(), call.string("id"))
            },
        )
    }
    obj("toolCallCancellation")?.let { cancellation ->
        return LiveServerToolCallCancellation(cancellation.strings("ids") ?: cancellation.strings("functionIds").orEmpty())
    }
    obj("goAway")?.let { goAway ->
        // A protobuf Duration ("12.5s"), or a number of seconds.
        val timeLeft = (goAway["timeLeft"] as? JsonPrimitive)?.let { value ->
            value.doubleOrNull ?: value.contentOrNull?.trim()?.takeIf { it.endsWith("s") }?.dropLast(1)?.toDoubleOrNull()
        }
        return LiveServerGoAway(timeLeft?.seconds)
    }
    obj("sessionResumptionUpdate")?.let { update ->
        return LiveSessionResumptionUpdate(update.string("newHandle"), update.boolean("resumable"), update.int("lastConsumedClientMessageIndex"))
    }
    return LiveServerUnknownMessage.create()
}

/** Parses [text] as a JSON object; anything else (a JSON `null` included) is an empty object. */
internal fun parseJsonObject(text: String?): JsonObject = text?.let { runCatching { Json.parseToJsonElement(it) }.getOrNull() as? JsonObject } ?: JsonObject(emptyMap())

/** The element, or null for JSON `null`. */
internal fun JsonElement.orNull(): JsonElement? = if (this is JsonNull) null else this

/** The JSON object of [element], or null. */
internal fun objectOrNull(element: JsonElement?): JsonObject? = runCatching { element?.jsonObject }.getOrNull()
