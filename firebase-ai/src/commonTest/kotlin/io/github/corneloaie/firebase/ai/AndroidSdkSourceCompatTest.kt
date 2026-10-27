/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.ai

import com.google.firebase.ai.InferenceMode
import com.google.firebase.ai.OnDeviceConfig
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.AutoFunctionDeclaration
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.FileDataPart
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionCallingConfig
import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.GenerationConfig
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.HarmBlockThreshold
import com.google.firebase.ai.type.HarmCategory
import com.google.firebase.ai.type.ImageSize
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.InvalidLocationException
import com.google.firebase.ai.type.InvalidStateException
import com.google.firebase.ai.type.JsonSchema
import com.google.firebase.ai.type.LatLng
import com.google.firebase.ai.type.Part
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.SafetySetting
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.StringFormat
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.ToolConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.asFileDataOrNull
import com.google.firebase.ai.type.asInlineDataPartOrNull
import com.google.firebase.ai.type.asTextOrNull
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.imageConfig
import com.google.firebase.ai.type.retrievalConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Code written against the Firebase AI Logic Android SDK's API, compiled from common code and run without a model: the
 * builders, schemas and value types. On Android the real SDK's classes run it (the module's are header stubs there),
 * so the expectations hold for both.
 */
class AndroidSdkSourceCompatTest {

    @Serializable
    data class City(val name: String, val population: Long)

    @Test
    fun contentBuilder() {
        val bytes = byteArrayOf(1, 2, 3)
        val content = content {
            text("Describe this image")
            inlineData(bytes, "image/png")
            fileData("gs://bucket/file.pdf", "application/pdf")
        }
        assertEquals("user", content.role)
        assertEquals(3, content.parts.size)
        assertEquals("Describe this image", content.parts[0].asTextOrNull())
        assertContentEquals(bytes, content.parts[1].asInlineDataPartOrNull()?.inlineData)
        assertEquals("image/png", (content.parts[1] as InlineDataPart).mimeType)
        assertEquals("gs://bucket/file.pdf", content.parts[2].asFileDataOrNull()?.uri)
        assertNull(content.parts[0].asInlineDataPartOrNull())

        val model = content("model") { text("Hi") }
        assertEquals("model", model.role)
        assertEquals(listOf("Hi"), model.parts.map { (it as TextPart).text })
        val copy = model.copy(role = "user")
        assertEquals("user", copy.role)
        assertEquals(model.parts, copy.parts)

        val built = Content.Builder().setRole("model").setParts(mutableListOf<Part>(TextPart("a"))).text("b").build()
        assertEquals("model", built.role)
        assertEquals(listOf("a", "b"), built.parts.map { it.asTextOrNull() })
        assertEquals("user", Content(parts = listOf(TextPart("x"))).role)
    }

    @Test
    fun parts() {
        val text = TextPart("hello")
        assertEquals("hello", text.text)
        assertEquals(false, text.isThought)
        assertNull(text.thoughtSignature)
        val thought = TextPart.createWithThinking("thinking", true, "signature")
        assertTrue(thought.isThought)
        assertEquals("signature", thought.thoughtSignature)

        val call = FunctionCallPart("getWeather", mapOf("city" to JsonPrimitive("Paris")), "call-1")
        assertEquals("getWeather", call.name)
        assertEquals(JsonPrimitive("Paris"), call.args["city"])
        assertEquals("call-1", call.id)

        val response = FunctionResponsePart.from(JsonObject(mapOf("temperature" to JsonPrimitive(21))))
        assertEquals(JsonPrimitive(21), response.response["temperature"])
        assertEquals("", response.name)
        assertEquals(emptyList(), response.parts)

        val file = FileDataPart("gs://bucket/a.png", "image/png")
        assertEquals("image/png", file.mimeType)
        val inline = InlineDataPart(byteArrayOf(9), "audio/pcm", "voice.pcm")
        assertEquals("voice.pcm", inline.displayName)
    }

    @Test
    fun schemas() {
        val schema = Schema.obj(
            properties = mapOf(
                "name" to Schema.string("The name", title = "Name"),
                "population" to Schema.long(minimum = 0.0),
                "country" to Schema.enumeration(listOf("FR", "DE"), nullable = true),
                "tags" to Schema.array(Schema.string(format = StringFormat.Custom("email")), minItems = 1),
                "capital" to Schema.boolean(),
                "area" to Schema.float(),
                "density" to Schema.double(),
                "rank" to Schema.integer(maximum = 10.0),
            ),
            optionalProperties = listOf("tags"),
            description = "A city",
        )
        assertEquals("OBJECT", schema.type)
        assertEquals("A city", schema.description)
        assertEquals(listOf("name", "population", "country", "capital", "area", "density", "rank"), schema.required)
        assertEquals("STRING", schema.properties!!.getValue("name").type)
        assertEquals("Name", schema.properties!!.getValue("name").title)
        assertEquals("INTEGER", schema.properties!!.getValue("population").type)
        assertEquals(0.0, schema.properties!!.getValue("population").minimum)
        assertEquals("enum", schema.properties!!.getValue("country").format)
        assertEquals(listOf("FR", "DE"), schema.properties!!.getValue("country").enum)
        assertEquals(true, schema.properties!!.getValue("country").nullable)
        assertEquals("ARRAY", schema.properties!!.getValue("tags").type)
        assertEquals("email", schema.properties!!.getValue("tags").items!!.format)
        assertEquals(1, schema.properties!!.getValue("tags").minItems)
        assertEquals("BOOLEAN", schema.properties!!.getValue("capital").type)
        assertEquals("NUMBER", schema.properties!!.getValue("area").type)
        assertEquals("float", schema.properties!!.getValue("area").format)
        assertEquals("NUMBER", schema.properties!!.getValue("density").type)
        assertEquals("int32", schema.properties!!.getValue("rank").format)
        assertEquals(10.0, schema.properties!!.getValue("rank").maximum)
        assertNull(Schema.anyOf(listOf(Schema.string(), Schema.integer())).properties)
        assertEquals(2, Schema.anyOf(listOf(Schema.string(), Schema.integer())).anyOf?.size)
        assertFailsWith<IllegalArgumentException> { Schema.obj(mapOf("a" to Schema.string()), optionalProperties = listOf("b")) }
    }

    @Test
    fun jsonSchemas() {
        val city = JsonSchema.obj(
            mapOf("name" to JsonSchema.string(), "population" to JsonSchema.long()),
            City::class,
            description = "A city",
        )
        assertEquals("OBJECT", city.type)
        assertEquals(City::class, city.clazz)
        assertEquals(listOf("name", "population"), city.required)
        assertEquals(City.serializer().descriptor.serialName, city.getSerializer().descriptor.serialName)
        val cities = JsonSchema.array(city, minItems = 2)
        assertEquals("ARRAY", cities.type)
        assertEquals(2, cities.minItems)
        assertEquals("int32", JsonSchema.integer().format)
        assertEquals("STRING", JsonSchema.enumeration(listOf("a", "b")).type)
        assertEquals("ANYOF", JsonSchema.anyOf(listOf(JsonSchema.string(), JsonSchema.boolean())).type)
        assertFailsWith<IllegalArgumentException> { JsonSchema.obj(mapOf("a" to JsonSchema.string()), optionalProperties = listOf("b")) }
    }

    @Test
    fun generationConfig() {
        val config = generationConfig {
            maxOutputTokens = 200
            stopSequences = listOf("END")
            responseMimeType = "application/json"
            responseSchema = Schema.string()
            responseModalities = listOf(ResponseModality.TEXT, ResponseModality.IMAGE)
            thinkingConfig = thinkingConfig { thinkingLevel = ThinkingLevel.LOW }
            imageConfig = imageConfig {
                aspectRatio = AspectRatio.LANDSCAPE_16x9
                imageSize = ImageSize.SIZE_2K
            }
            speechConfig = SpeechConfig(Voice("Kore"), "en-US")
        }
        // toBuilder keeps the settings, and the schemas stay mutually exclusive.
        assertFailsWith<InvalidStateException> { config.toBuilder().setResponseSchemaJson(JsonSchema.string()).build() }
        GenerationConfig.builder().setResponseSchemaJson(JsonSchema.string()).setMaxOutputTokens(10).build()
        assertFailsWith<IllegalArgumentException> {
            thinkingConfig {
                thinkingBudget = 100
                thinkingLevel = ThinkingLevel.HIGH
            }
        }
    }

    @Test
    fun backendsAndOptions() {
        assertEquals(GenerativeBackend.googleAI(), GenerativeBackend.googleAI())
        assertEquals(GenerativeBackend.agentPlatform("europe-west4"), GenerativeBackend.agentPlatform("europe-west4"))
        assertNotEquals(GenerativeBackend.agentPlatform("europe-west4"), GenerativeBackend.agentPlatform("us-central1"))
        assertNotEquals(GenerativeBackend.googleAI(), GenerativeBackend.agentPlatform())
        assertFailsWith<InvalidLocationException> { GenerativeBackend.agentPlatform("") }
        assertFailsWith<InvalidLocationException> { GenerativeBackend.agentPlatform("projects/p") }
        RequestOptions()
        RequestOptions(timeoutInMillis = 5_000)
        RequestOptions(timeoutInMillis = 5_000, autoFunctionCallingTurnLimit = 3)
        assertEquals(InferenceMode.ONLY_IN_CLOUD, OnDeviceConfig.IN_CLOUD.mode)
        assertEquals(1, OnDeviceConfig(InferenceMode.PREFER_ON_DEVICE).candidateCount)
    }

    @Test
    fun toolsAndSafety() {
        val declaration = FunctionDeclaration("getWeather", "The weather of a city", mapOf("city" to Schema.string()))
        Tool.functionDeclarations(listOf(declaration))
        Tool.googleSearch()
        Tool.codeExecution()
        Tool.urlContext()
        Tool.googleMaps()
        val auto = AutoFunctionDeclaration.create(
            functionName = "lookup",
            description = "Looks a city up",
            inputSchema = JsonSchema.obj(mapOf("name" to JsonSchema.string(), "population" to JsonSchema.long()), City::class),
        ) { city: City -> FunctionResponsePart.from(JsonObject(mapOf("found" to JsonPrimitive(city.name)))) }
        assertEquals("lookup", auto.name)
        Tool.functionDeclarations(autoFunctionDeclarations = listOf(auto))
        ToolConfig(FunctionCallingConfig.any(listOf("getWeather")), retrievalConfig { latLng = LatLng(48.85, 2.35) })
        ToolConfig(FunctionCallingConfig.none())
        SafetySetting(HarmCategory.HARASSMENT, HarmBlockThreshold.ONLY_HIGH)
        assertIs<LatLng>(LatLng(1.0, 2.0).copy(latitude = 3.0))
    }
}
