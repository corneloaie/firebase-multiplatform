/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.ai.internal

import com.google.firebase.ai.InferenceSource
import com.google.firebase.ai.type.APINotConfiguredException
import com.google.firebase.ai.type.BlockReason
import com.google.firebase.ai.type.ContentModality
import com.google.firebase.ai.type.FinishReason
import com.google.firebase.ai.type.FirebaseAutoFunctionException
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerateObjectResponse
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.HarmProbability
import com.google.firebase.ai.type.HarmSeverity
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.InvalidAPIKeyException
import com.google.firebase.ai.type.JsonSchema
import com.google.firebase.ai.type.LiveServerContent
import com.google.firebase.ai.type.LiveServerGoAway
import com.google.firebase.ai.type.LiveServerSetupComplete
import com.google.firebase.ai.type.LiveServerToolCall
import com.google.firebase.ai.type.LiveServerToolCallCancellation
import com.google.firebase.ai.type.LiveServerUnknownMessage
import com.google.firebase.ai.type.LiveSessionResumptionUpdate
import com.google.firebase.ai.type.PromptBlockedException
import com.google.firebase.ai.type.QuotaExceededException
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ResponseStoppedException
import com.google.firebase.ai.type.SerializationException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.ServiceDisabledException
import com.google.firebase.ai.type.SpeechMetadata
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.UnknownException
import com.google.firebase.ai.type.UnsupportedUserLocationException
import com.google.firebase.ai.type.UrlRetrievalStatus
import io.github.corneloaie.firebase.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.seconds

/** The responses, as the platform SDKs hand them over, decoded the way the Android SDK decodes them. */
class ResponseJsonTest {

    private fun json(text: String): JsonObject = Json.parseToJsonElement(text).jsonObject

    private val response = json(
        """
        {
          "candidates": [{
            "content": {"role": "model", "parts": [
              {"text": "Let me think", "thought": true, "thoughtSignature": "sig"},
              {"text": "Paris is "},
              {"text": "the capital"},
              {"text": ""},
              {"inlineData": {"mimeType": "image/png", "data": "AQID"}},
              {"functionCall": {"name": "lookup", "args": {"q": "Paris"}, "id": "c1"}},
              {"somethingNew": {}}
            ]},
            "finishReason": "STOP",
            "safetyRatings": [
              {"category": "HARM_CATEGORY_HARASSMENT", "probability": "LOW", "probabilityScore": 0.25, "severity": "HARM_SEVERITY_LOW", "severityScore": 0.5},
              {"category": "HARM_CATEGORY_DANGEROUS_CONTENT"}
            ],
            "citationMetadata": {"citationSources": [{"startIndex": 0, "endIndex": 9, "uri": "https://example.com", "publicationDate": {"year": 2024, "month": 2, "day": 3}}]},
            "groundingMetadata": {
              "webSearchQueries": ["capital of france"],
              "searchEntryPoint": {"renderedContent": "<div/>"},
              "groundingChunks": [{"web": {"uri": "https://a", "title": "A"}}, {"maps": {"placeId": "places/1"}}],
              "groundingSupports": [{"segment": {"partIndex": 1, "startIndex": 0, "endIndex": 5, "text": "Paris"}, "groundingChunkIndices": [0]}]
            },
            "urlContextMetadata": {"urlMetadata": [{"retrievedUrl": "https://a", "urlRetrievalStatus": "URL_RETRIEVAL_STATUS_SUCCESS"}]}
          }],
          "usageMetadata": {"promptTokenCount": 3, "candidatesTokenCount": 4, "totalTokenCount": 9, "thoughtsTokenCount": 2,
            "promptTokensDetails": [{"modality": "TEXT", "tokenCount": 3}]},
          "modelVersion": "gemini-test"
        }
        """,
    ).toGenerateContentResponse()

    @Test
    fun generateContentResponse() {
        val candidate = response.candidates.single()
        // The unknown part and the empty text are dropped.
        assertEquals(5, candidate.content.parts.size)
        assertEquals("model", candidate.content.role)
        assertEquals("Paris is  the capital", response.text)
        assertEquals("Let me think", response.thoughtSummary)
        assertEquals("sig", (candidate.content.parts.first() as TextPart).thoughtSignature)
        assertContentEquals(byteArrayOf(1, 2, 3), response.inlineDataParts.single().inlineData)
        assertIs<InlineDataPart>(candidate.content.parts[3])
        val call = response.functionCalls.single()
        assertEquals("lookup", call.name)
        assertEquals(JsonPrimitive("Paris"), call.args["q"])
        assertEquals("c1", call.id)
        assertEquals(FinishReason.STOP, candidate.finishReason)
        assertSame(InferenceSource.IN_CLOUD, response.inferenceSource)
        assertEquals("gemini-test", response.modelVersion)

        // A rating without a probability is dropped, as the Android SDK does.
        val rating = candidate.safetyRatings.single()
        assertEquals(HarmCategory.HARASSMENT, rating.category)
        assertEquals(HarmProbability.LOW, rating.probability)
        assertEquals(0.25f, rating.probabilityScore)
        assertEquals(HarmSeverity.LOW, rating.severity)
        assertEquals(0.5f, rating.severityScore)

        // The Google Developer API's citationSources; the UTF-8 end index is converted to UTF-16 over the text parts.
        val citation = candidate.citationMetadata!!.citations.single()
        assertEquals("https://example.com", citation.uri)
        assertEquals(0, citation.startIndex)
        assertEquals(2024, citation.publicationYear)
        assertEquals(2, citation.publicationMonth)

        val grounding = candidate.groundingMetadata!!
        assertEquals(listOf("capital of france"), grounding.webSearchQueries)
        assertEquals("<div/>", grounding.searchEntryPoint!!.renderedContent)
        assertEquals("A", grounding.groundingChunks[0].web!!.title)
        assertEquals("places/1", grounding.groundingChunks[1].maps!!.placeId)
        assertEquals("Paris", grounding.groundingSupports.single().segment.text)
        assertEquals(listOf(0), grounding.groundingSupports.single().groundingChunkIndices)
        assertEquals(UrlRetrievalStatus.SUCCESS, candidate.urlContextMetadata!!.urlMetadata.single().urlRetrievalStatus)

        val usage = response.usageMetadata!!
        assertEquals(3, usage.promptTokenCount)
        assertEquals(4, usage.candidatesTokenCount)
        assertEquals(9, usage.totalTokenCount)
        assertEquals(2, usage.thoughtsTokenCount)
        assertEquals(0, usage.cachedContentTokenCount)
        assertEquals(ContentModality.TEXT, usage.promptTokensDetails.single().modality)
        response.validate()
    }

    @Test
    fun utf8IndicesBecomeUtf16() {
        val content = json("""{"role": "model", "parts": [{"text": "héllo 🌍!"}]}""").toContent()
        // "h" 1 byte, "é" 2, "llo " 4, "🌍" 4 (two UTF-16 chars), "!" 1.
        assertEquals(1, convertUtf8IndexToUtf16(content, 1))
        assertEquals(2, convertUtf8IndexToUtf16(content, 3))
        assertEquals(8, convertUtf8IndexToUtf16(content, 11))
        assertEquals(9, convertUtf8IndexToUtf16(content, 12))
    }

    @Test
    fun emptyContentKeepsASpace() {
        assertEquals(listOf(" "), json("""{"role": "model", "parts": [{"text": ""}]}""").toContent().parts.map { (it as TextPart).text })
        assertEquals(emptyList(), json("""{"role": "model", "parts": [{"other": 1}]}""").toContent().parts)
    }

    @Test
    fun speechMetadata() {
        val parts = json("""{"role": "model", "parts": [{"text": "Hello", "speechMetadata": {"speaker": "Joe", "style": "cheerfully"}}, {"text": "Hi"}]}""").toContent().parts
        val metadata: List<SpeechMetadata?> = parts.map { (it as TextPart).speechMetadata }
        assertEquals(listOf("Joe" to "cheerfully", null), metadata.map { it?.let { m -> m.speaker to m.style } })
    }

    @Test
    fun validation() {
        val blocked = json("""{"promptFeedback": {"blockReason": "SAFETY", "safetyRatings": []}}""").toGenerateContentResponse()
        assertEquals(BlockReason.SAFETY, blocked.promptFeedback!!.blockReason)
        assertSame(blocked, assertFailsWith<PromptBlockedException> { blocked.validate() }.response)
        val stopped = json("""{"candidates": [{"content": {"role": "model", "parts": [{"text": "a"}]}, "finishReason": "MAX_TOKENS"}]}""").toGenerateContentResponse()
        assertSame(stopped, assertFailsWith<ResponseStoppedException> { stopped.validate() }.response)
        assertFailsWith<SerializationException> { json("{}").toGenerateContentResponse().validate() }
        assertEquals(FinishReason.UNKNOWN, json("""{"candidates": [{"finishReason": "SOMETHING_NEW"}]}""").toGenerateContentResponse().candidates.single().finishReason)
    }

    @Test
    fun countTokens() {
        val count = json("""{"totalTokens": 7, "promptTokensDetails": [{"modality": "IMAGE", "tokenCount": 7}]}""").toCountTokensResponse()
        val (total, details) = count
        assertEquals(7, total)
        assertEquals(ContentModality.IMAGE, details.single().modality)
        assertEquals(0, json("{}").toCountTokensResponse().totalTokens)
    }

    @Test
    fun liveServerMessages() {
        val content = json("""{"serverContent": {"modelTurn": {"role": "model", "parts": [{"text": "Hi"}]}, "turnComplete": true, "outputTranscription": {"text": "hi"}}}""").toLiveServerMessage()
        assertIs<LiveServerContent>(content)
        assertEquals("Hi", (content.content!!.parts.single() as TextPart).text)
        assertEquals(true, content.turnComplete)
        assertEquals(false, content.interrupted)
        assertEquals("hi", content.outputTranscription!!.text)
        assertSame(LiveServerSetupComplete, json("""{"setupComplete": {}}""").toLiveServerMessage())
        val toolCall = json("""{"toolCall": {"functionCalls": [{"name": "f", "args": {"a": 1}, "id": "x"}]}}""").toLiveServerMessage()
        assertEquals("x", (toolCall as LiveServerToolCall).functionCalls.single().id)
        assertEquals(listOf("x"), (json("""{"toolCallCancellation": {"ids": ["x"]}}""").toLiveServerMessage() as LiveServerToolCallCancellation).functionIds)
        assertEquals(1.5.seconds, (json("""{"goAway": {"timeLeft": "1.5s"}}""").toLiveServerMessage() as LiveServerGoAway).timeLeft)
        assertEquals(2.seconds, (json("""{"goAway": {"timeLeft": 2}}""").toLiveServerMessage() as LiveServerGoAway).timeLeft)
        val update = json("""{"sessionResumptionUpdate": {"newHandle": "h", "resumable": true}}""").toLiveServerMessage()
        assertEquals("h", (update as LiveSessionResumptionUpdate).newHandle)
        assertIs<LiveServerUnknownMessage>(json("""{"somethingNew": {}}""").toLiveServerMessage())
    }

    @Test
    fun errors() {
        fun server(message: String, vararg details: JsonObject) = firebaseAIException(NativeAIError(NativeAIError.Kind.SERVER, message, 400, details.toList()))
        assertIs<InvalidAPIKeyException>(server("API key not valid. Please pass a valid API key."))
        assertIs<UnsupportedUserLocationException>(server("User location is not supported for the API use."))
        assertIs<QuotaExceededException>(server("Resource has been exhausted (e.g. check quota)."))
        assertIs<PromptBlockedException>(server("The prompt could not be submitted."))
        assertIs<APINotConfiguredException>(server("genai config not found"))
        assertIs<ServiceDisabledException>(server("API has not been used", json("""{"reason": "SERVICE_DISABLED", "domain": "googleapis.com"}""")))
        assertIs<ServerException>(server("Internal error"))
        assertIs<RequestTimeoutException>(firebaseAIException(NativeAIError(NativeAIError.Kind.TIMEOUT, "timed out")))
        assertIs<UnknownException>(firebaseAIException(IllegalStateException("?")))
        val serverError = server("Internal error")
        assertSame(serverError, firebaseAIException(serverError))
    }

    @Serializable
    data class Query(val q: String)

    @Serializable
    data class Answer(val text: String)

    @Test
    fun automaticFunctions() = runTest {
        val call = FunctionCallPart("answer", mapOf("q" to JsonPrimitive("hi")), "id-1")
        val answer: suspend (Query) -> Answer = { query -> Answer("You said ${query.q}") }
        val response = executeAutoFunction(
            call,
            "answer",
            JsonSchema.obj(mapOf("q" to JsonSchema.string()), Query::class),
            JsonSchema.obj(mapOf("text" to JsonSchema.string()), Answer::class),
            answer,
        )
        assertEquals("answer", response.name)
        assertEquals("id-1", response.id)
        assertEquals(JsonPrimitive("You said hi"), response.response["text"])
        val failing: suspend (Query) -> FunctionResponsePart = { throw FirebaseAutoFunctionException("nope") }
        val failed = executeAutoFunction(call, "answer", JsonSchema.obj(mapOf("q" to JsonSchema.string()), Query::class), null, failing)
        assertEquals(JsonPrimitive("nope"), failed.response["error"])
        val responding: suspend (Query) -> FunctionResponsePart = { FunctionResponsePart.from(JsonObject(mapOf("ok" to JsonPrimitive(true)))) }
        val direct = executeAutoFunction(call, "answer", JsonSchema.obj(mapOf("q" to JsonSchema.string()), Query::class), null, responding)
        assertEquals("id-1", direct.id)
        assertEquals(JsonPrimitive(true), direct.response["ok"])
    }

    @Test
    fun generateObjectResponse() {
        val objects = json("""{"candidates": [{"content": {"role": "model", "parts": [{"text": "{\"text\": \"hello\"}"}]}, "finishReason": "STOP"}]}""").toGenerateContentResponse()
        val typed = GenerateObjectResponse(objects, JsonSchema.obj(mapOf("text" to JsonSchema.string()), Answer::class))
        assertEquals(Answer("hello"), typed.getObject())
        assertNull(typed.getObject(1))
    }
}
