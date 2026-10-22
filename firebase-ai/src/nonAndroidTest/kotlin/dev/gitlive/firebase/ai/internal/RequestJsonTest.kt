/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.ai.internal

import com.google.firebase.ai.type.AutoFunctionDeclaration
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ContextWindowCompressionConfig
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionCallingConfig
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.HarmBlockMethod
import com.google.firebase.ai.type.HarmBlockThreshold
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.InvalidStateException
import com.google.firebase.ai.type.JsonSchema
import com.google.firebase.ai.type.LatLng
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SafetySetting
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SessionResumptionConfig
import com.google.firebase.ai.type.SlidingWindow
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.SpeechMetadata
import com.google.firebase.ai.type.TemplateFunctionDeclaration
import com.google.firebase.ai.type.TemplateTool
import com.google.firebase.ai.type.TemplateToolConfig
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.ToolConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.liveGenerationConfig
import com.google.firebase.ai.type.retrievalConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertFailsWith

/** The requests as the JSON of the REST API, which the platform SDKs are handed. */
class RequestJsonTest {

    @Test
    fun generateContentRequest() {
        val request = generateContentRequest(
            contents = listOf(
                content {
                    text("What is in this picture?")
                    inlineData(byteArrayOf(1, 2, 3), "image/png")
                    fileData("gs://bucket/a.pdf", "application/pdf")
                },
                Content("model", listOf(FunctionCallPart("lookup", mapOf("q" to JsonPrimitive("x")), "1"))),
                Content("user", listOf(FunctionResponsePart("lookup", JsonObject(mapOf("found" to JsonPrimitive(true))), "1"))),
            ),
            safetySettings = listOf(SafetySetting(HarmCategory.HATE_SPEECH, HarmBlockThreshold.LOW_AND_ABOVE, HarmBlockMethod.SEVERITY)),
            generationConfig = generationConfig {
                maxOutputTokens = 100
                responseModalities = listOf(ResponseModality.TEXT)
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.MEDIUM
                    includeThoughts = true
                }
                speechConfig = SpeechConfig(Voice("Kore"))
            },
            tools = listOf(Tool.googleSearch()),
            toolConfig = ToolConfig(FunctionCallingConfig.any(listOf("lookup")), retrievalConfig { languageCode = "en" }),
            systemInstruction = content { text("Be brief") },
        )
        assertJson(
            """
                {
                  "contents": [
                    {"role": "user", "parts": [
                      {"text": "What is in this picture?", "thought": false},
                      {"inlineData": {"mimeType": "image/png", "data": "AQID"}, "thought": false},
                      {"fileData": {"mimeType": "application/pdf", "fileUri": "gs://bucket/a.pdf"}, "thought": false}
                    ]},
                    {"role": "model", "parts": [{"functionCall": {"name": "lookup", "args": {"q": "x"}, "id": "1"}, "thought": false}]},
                    {"role": "user", "parts": [{"functionResponse": {"name": "lookup", "response": {"found": true}, "id": "1"}, "thought": false}]}
                  ],
                  "safetySettings": [{"category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_LOW_AND_ABOVE", "method": "SEVERITY"}],
                  "generationConfig": {
                    "maxOutputTokens": 100,
                    "responseModalities": ["TEXT"],
                    "thinkingConfig": {"includeThoughts": true, "thinkingLevel": "MEDIUM"},
                    "speechConfig": {"voiceConfig": {"prebuiltVoiceConfig": {"voiceName": "Kore"}}}
                  },
                  "tools": [{"functionDeclarations": [], "googleSearch": {}}],
                  "toolConfig": {"functionCallingConfig": {"mode": "ANY", "allowedFunctionNames": ["lookup"]}, "retrievalConfig": {"languageCode": "en"}},
                  "systemInstruction": {"role": "system", "parts": [{"text": "Be brief", "thought": false}]}
                }
                """,
            request,
        )
    }

    @Test
    fun harmBlockMethodIsNotForGoogleAI() {
        val settings = listOf(SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.NONE, HarmBlockMethod.PROBABILITY))
        assertFailsWith<InvalidStateException> { checkSafetySettingsFor(googleAI = true, safetySettings = settings) }
        checkSafetySettingsFor(googleAI = false, safetySettings = settings)
    }

    @Test
    fun countTokensRequestLeavesOutSafetyAndToolConfig() {
        val request = countTokensRequest(listOf(content { text("Hi") }), generationConfig { maxOutputTokens = 5 }, emptyList(), null)
        assertJson("""{"contents": [{"role": "user", "parts": [{"text": "Hi", "thought": false}]}], "generationConfig": {"maxOutputTokens": 5}}""", request)
    }

    @Test
    fun speechMetadata() {
        val contents = listOf(
            content {
                text("Hello", SpeechMetadata("Joe", "cheerfully"))
                text("Hi", SpeechMetadata(speaker = "Jane"))
            },
        )
        val request = countTokensRequest(contents, generationConfig { maxOutputTokens = 5 }, emptyList(), null)
        assertJson(
            """
            {"contents": [{"role": "user", "parts": [
              {"text": "Hello", "thought": false, "speechMetadata": {"speaker": "Joe", "style": "cheerfully"}},
              {"text": "Hi", "thought": false, "speechMetadata": {"speaker": "Jane"}}
            ]}], "generationConfig": {"maxOutputTokens": 5}}
            """,
            request,
        )
    }

    @Test
    fun schemas() {
        val schema = Schema.obj(
            mapOf(
                "name" to Schema.string(nullable = true),
                "rank" to Schema.integer(),
                "kind" to Schema.enumeration(listOf("a", "b")),
                "either" to Schema.anyOf(listOf(Schema.boolean(), Schema.float())),
            ),
            optionalProperties = listOf("either"),
        )
        assertJson(
            """
                {"type": "OBJECT", "nullable": false, "properties": {
                  "name": {"type": "STRING", "nullable": true},
                  "rank": {"type": "INTEGER", "format": "int32", "nullable": false},
                  "kind": {"type": "STRING", "format": "enum", "nullable": false, "enum": ["a", "b"]},
                  "either": {"anyOf": [{"type": "BOOLEAN", "nullable": false}, {"type": "NUMBER", "format": "float", "nullable": false}]}
                }, "required": ["name", "rank", "kind"]}
                """,
            schema.toOpenApiJson(),
        )
        assertJson(
            """
                {"type": "object", "properties": {
                  "name": {"type": ["string", "null"]},
                  "rank": {"type": "integer", "minimum": -2147483648.0, "maximum": 2147483647.0},
                  "kind": {"enum": ["a", "b"]},
                  "either": {"anyOf": [{"type": "boolean"}, {"type": "number"}]}
                }, "required": ["name", "rank", "kind"]}
                """,
            schema.toJsonSchemaJson(),
        )
        // An enumeration has no type in a JSON schema; a nullable one allows "null".
        assertJson("""{"enum": ["x", "null"]}""", JsonSchema.enumeration(listOf("x"), nullable = true).toJsonSchemaJson())
    }

    @Test
    fun tools() {
        val declaration = FunctionDeclaration("weather", "The weather", mapOf("city" to Schema.string()), optionalParameters = emptyList())
        val auto = AutoFunctionDeclaration.create<String>("echo", "Echoes", JsonSchema.string()) { FunctionResponsePart.from(JsonObject(emptyMap())) }
        assertJson(
            """
                {"functionDeclarations": [
                  {"name": "weather", "description": "The weather", "parameters": {"type": "OBJECT", "nullable": false, "properties": {"city": {"type": "STRING", "nullable": false}}, "required": ["city"]}},
                  {"name": "echo", "description": "Echoes", "parametersJsonSchema": {"type": "string"}}
                ]}
                """,
            Tool.functionDeclarations(listOf(declaration), listOf(auto)).toJson(),
        )
        assertJson("""{"functionDeclarations": [], "codeExecution": {}}""", Tool.codeExecution().toJson())
        assertJson("""{"functionDeclarations": [], "urlContext": {}}""", Tool.urlContext().toJson())
        assertJson("""{"functionDeclarations": [], "googleMaps": {}}""", Tool.googleMaps().toJson())
        assertJson(
            """{"retrievalConfig": {"latLng": {"latitude": 1.5, "longitude": 2.5}}}""",
            ToolConfig(retrievalConfig = retrievalConfig { latLng = LatLng(1.5, 2.5) }).toJson(),
        )
    }

    @Test
    fun templateRequest() {
        val request = templateGenerateContentRequest(
            inputs = mapOf("name" to "Ada", "age" to 36, "tags" to listOf("a", "b"), "nested" to mapOf("ok" to true)),
            history = listOf(Content("user", listOf(TextPart.createWithThinking("Hi", true, "sig")))),
            tools = listOf(TemplateTool.functionDeclarations(listOf(TemplateFunctionDeclaration("f", JsonSchema.string())))),
            toolConfig = TemplateToolConfig(retrievalConfig { languageCode = "fr" }),
        )
        assertJson(
            """
                {
                  "inputs": {"name": "Ada", "age": 36, "tags": ["a", "b"], "nested": {"ok": true}},
                  "history": [{"role": "user", "parts": [{"text": "Hi", "thoughtSignature": "sig"}]}],
                  "tools": [{"templateFunctions": [{"name": "f", "inputSchema": {"type": "string"}}]}],
                  "toolConfig": {"retrievalConfig": {"languageCode": "fr"}}
                }
                """,
            request,
        )
    }

    @Test
    fun liveMessages() {
        val config = liveGenerationConfig {
            temperature = 0.5f
            responseModality = ResponseModality.AUDIO
            speechConfig = SpeechConfig(Voice("Puck"), "en-US")
            inputAudioTranscription = com.google.firebase.ai.type.AudioTranscriptionConfig()
            contextWindowCompression = ContextWindowCompressionConfig(triggerTokens = 1000, slidingWindow = SlidingWindow(500))
        }
        assertJson(
            """
                {
                  "generationConfig": {"temperature": 0.5, "speechConfig": {"voiceConfig": {"prebuiltVoiceConfig": {"voiceName": "Puck"}}, "languageCode": "en-US"}, "responseModalities": ["AUDIO"]},
                  "systemInstruction": {"role": "user", "parts": [{"text": "Hi", "thought": false}]},
                  "inputAudioTranscription": {},
                  "sessionResumption": {"handle": "h"},
                  "contextWindowCompression": {"triggerTokens": 1000, "slidingWindow": {"targetTokens": 500}}
                }
                """,
            liveSetup(config, emptyList(), content { text("Hi") }, SessionResumptionConfig("h")),
        )
        assertJson("""{"audio": {"mimeType": "audio/pcm", "data": "AAE="}}""", realtimeInput(audio = InlineData(byteArrayOf(0, 1), "audio/pcm")))
        assertJson("""{"activityStart": {}}""", realtimeInput(activityStart = true))
        assertJson(
            """[{"name": "f", "response": {"a": 1}, "id": "7"}]""",
            toolResponse(listOf(FunctionResponsePart("f", JsonObject(mapOf("a" to JsonPrimitive(1))), "7"))),
        )
    }
}
