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
import io.github.corneloaie.firebase.ai.internal.stub

// Header stubs of the Android SDK's FirebaseAI (see utils.stripHeaderStubs).

public actual class FirebaseAI private constructor() {
    @JvmOverloads
    public actual fun generativeModel(
        modelName: String,
        generationConfig: GenerationConfig?,
        safetySettings: List<SafetySetting>?,
        tools: List<Tool>?,
        toolConfig: ToolConfig?,
        systemInstruction: Content?,
        requestOptions: RequestOptions,
    ): GenerativeModel = stub()

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
    ): GenerativeModel = stub()

    @JvmOverloads
    @PublicPreviewAPI
    public actual fun templateGenerativeModel(
        requestOptions: RequestOptions,
        tools: List<TemplateTool>?,
        toolConfig: TemplateToolConfig?,
    ): TemplateGenerativeModel = stub()

    @JvmOverloads
    @PublicPreviewAPI
    public actual fun liveModel(
        modelName: String,
        generationConfig: LiveGenerationConfig?,
        tools: List<Tool>?,
        systemInstruction: Content?,
        requestOptions: RequestOptions,
    ): LiveGenerativeModel = stub()

    public actual companion object {
        @JvmStatic
        public actual val instance: FirebaseAI get() = stub()

        @JvmStatic
        @JvmOverloads
        public actual fun getInstance(app: FirebaseApp, backend: GenerativeBackend): FirebaseAI = stub()

        @JvmStatic
        @JvmOverloads
        public actual fun getInstance(app: FirebaseApp, backend: GenerativeBackend, useLimitedUseAppCheckTokens: Boolean): FirebaseAI = stub()

        @JvmStatic
        public actual fun getInstance(app: FirebaseApp): FirebaseAI = stub()
    }
}
