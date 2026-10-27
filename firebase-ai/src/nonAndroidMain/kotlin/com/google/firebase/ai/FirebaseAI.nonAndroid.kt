/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.ai

import com.google.firebase.FirebaseApp
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerationConfig
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.LiveGenerationConfig
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.SafetySetting
import com.google.firebase.ai.type.TemplateTool
import com.google.firebase.ai.type.TemplateToolConfig
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.ToolConfig
import io.github.corneloaie.firebase.ai.internal.NativeAI

public actual class FirebaseAI internal constructor(
    private val app: FirebaseApp,
    private val backend: GenerativeBackend,
    private val useLimitedUseAppCheckTokens: Boolean,
) {
    private val native: NativeAI by lazy { NativeAI(app, backend, useLimitedUseAppCheckTokens) }

    public actual fun generativeModel(
        modelName: String,
        generationConfig: GenerationConfig?,
        safetySettings: List<SafetySetting>?,
        tools: List<Tool>?,
        toolConfig: ToolConfig?,
        systemInstruction: Content?,
        requestOptions: RequestOptions,
    ): GenerativeModel = generativeModel(modelName, generationConfig, safetySettings, tools, toolConfig, systemInstruction, requestOptions, OnDeviceConfig.IN_CLOUD)

    @PublicPreviewAPI
    public actual fun generativeModel(
        modelName: String,
        generationConfig: GenerationConfig?,
        safetySettings: List<SafetySetting>?,
        tools: List<Tool>?,
        toolConfig: ToolConfig?,
        systemInstruction: Content?,
        requestOptions: RequestOptions,
        onDeviceConfig: OnDeviceConfig,
    ): GenerativeModel = GenerativeModel(
        native = native,
        backend = backend,
        modelName = modelName,
        generationConfig = generationConfig,
        safetySettings = safetySettings,
        tools = tools.orEmpty(),
        toolConfig = toolConfig,
        systemInstruction = systemInstruction,
        requestOptions = requestOptions,
        onDeviceConfig = onDeviceConfig,
    )

    @PublicPreviewAPI
    public actual fun templateGenerativeModel(
        requestOptions: RequestOptions,
        tools: List<TemplateTool>?,
        toolConfig: TemplateToolConfig?,
    ): TemplateGenerativeModel = TemplateGenerativeModel(native, requestOptions, tools, toolConfig)

    @PublicPreviewAPI
    public actual fun liveModel(
        modelName: String,
        generationConfig: LiveGenerationConfig?,
        tools: List<Tool>?,
        systemInstruction: Content?,
        requestOptions: RequestOptions,
    ): LiveGenerativeModel = LiveGenerativeModel(native, modelName, generationConfig, tools.orEmpty(), systemInstruction, requestOptions)

    public actual companion object {
        /** The instances, one per app, backend and App Check token setting, as the Android SDK keeps them. */
        private val instances = mutableMapOf<Triple<String, GenerativeBackend, Boolean>, FirebaseAI>()

        public actual val instance: FirebaseAI get() = getInstance(backend = GenerativeBackend.googleAI())

        public actual fun getInstance(app: FirebaseApp, backend: GenerativeBackend): FirebaseAI = getInstance(app, backend, false)

        public actual fun getInstance(app: FirebaseApp, backend: GenerativeBackend, useLimitedUseAppCheckTokens: Boolean): FirebaseAI = instances.getOrPut(Triple(app.name, backend, useLimitedUseAppCheckTokens)) { FirebaseAI(app, backend, useLimitedUseAppCheckTokens) }

        public actual fun getInstance(app: FirebaseApp): FirebaseAI = getInstance(app, GenerativeBackend.googleAI())
    }
}
