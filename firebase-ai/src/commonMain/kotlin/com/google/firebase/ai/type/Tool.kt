/*
 * Copyright 2024 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modified in firebase-multiplatform for Kotlin Multiplatform: the public API of the Firebase AI Logic
 * Android SDK without its Android implementation (see firebase-ai/documentation.md).
 */
package com.google.firebase.ai.type

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlinx.serialization.json.JsonObject

/**
 * Contains a set of tools (like function declarations) that the model has access to. These tools
 * can be used to gather information or complete tasks.
 */
public class Tool
@OptIn(PublicPreviewAPI::class)
internal constructor(
    internal val functionDeclarations: List<FunctionDeclaration>?,
    internal val autoFunctionDeclarations: List<AutoFunctionDeclaration<*, *>>?,
    internal val googleSearch: GoogleSearch?,
    internal val codeExecution: JsonObject?,
    internal val urlContext: UrlContext?,
    internal val googleMaps: GoogleMaps?,
) {
    public companion object {
        @OptIn(PublicPreviewAPI::class)
        internal val codeExecutionInstance by lazy {
            Tool(null, null, null, JsonObject(emptyMap()), null, null)
        }

        /**
         * Creates a [Tool] instance that provides the model with access to the [functionDeclarations].
         *
         * @param functionDeclarations The list of functions that this tool allows the model access to.
         */
        @JvmStatic
        public fun functionDeclarations(
            functionDeclarations: List<FunctionDeclaration>,
        ): Tool {
            @OptIn(PublicPreviewAPI::class)
            return Tool(functionDeclarations, null, null, null, null, null)
        }

        /**
         * Creates a [Tool] instance that provides the model with access to the [functionDeclarations].
         *
         * @param functionDeclarations The list of functions that this tool allows the model access to.
         * @param autoFunctionDeclarations The list of functions that this tool has access to which
         * should be executed automatically
         */
        @JvmStatic
        public fun functionDeclarations(
            functionDeclarations: List<FunctionDeclaration>? = null,
            autoFunctionDeclarations: List<AutoFunctionDeclaration<*, *>>?,
        ): Tool {
            @OptIn(PublicPreviewAPI::class)
            return Tool(functionDeclarations, autoFunctionDeclarations, null, null, null, null)
        }

        /** Creates a [Tool] instance that allows the model to use code execution. */
        @JvmStatic
        public fun codeExecution(): Tool = codeExecutionInstance

        /**
         * Creates a [Tool] instance that allows you to provide additional context to the models in the
         * form of public web URLs. By including URLs in your request, the Gemini model will access the
         * content from those pages to inform and enhance its response.
         *
         * @param urlContext Specifies the URL context configuration.
         * @return A [Tool] configured for URL context.
         */
        @JvmStatic
        @JvmOverloads
        public fun urlContext(urlContext: UrlContext = UrlContext()): Tool = Tool(null, null, null, null, urlContext, null)

        /**
         * Creates a [Tool] instance that allows the model to use Grounding with Google Search.
         *
         * Grounding with Google Search can be used to allow the model to connect to Google Search to
         * access and incorporate up-to-date information from the web into its responses.
         *
         * When using this feature, you are required to comply with the "Grounding with Google Search"
         * usage requirements for your chosen API provider:
         * [Gemini Developer API](https://ai.google.dev/gemini-api/terms#grounding-with-google-search)
         * or Agent Platform Gemini API (see
         * [Service Terms](https://cloud.google.com/terms/service-terms) section within the Service
         * Specific Terms).
         *
         * @param googleSearch An empty [GoogleSearch] object. The presence of this object in the list
         * of tools enables the model to use Google Search.
         * @return A [Tool] configured for Google Search.
         */
        @JvmStatic
        @JvmOverloads
        public fun googleSearch(googleSearch: GoogleSearch = GoogleSearch()): Tool {
            @OptIn(PublicPreviewAPI::class)
            return Tool(null, null, googleSearch, null, null, null)
        }

        /**
         * Creates a [Tool] instance that allows the model to use Grounding with Google Maps.
         *
         * Grounding with Google Maps can be used to allow the model to connect to Google Maps to
         * incorporate location-based information into its responses.
         *
         * When using this feature, you are required to comply with the "Grounding with Google Maps"
         * usage requirements for your chosen API provider:
         * [Gemini Developer API](https://ai.google.dev/gemini-api/terms#grounding-with-google-maps) or
         * Agent Platform Gemini API (see [Service Terms](https://cloud.google.com/terms/service-terms)
         * section within the Service Specific Terms).
         *
         * @return A [Tool] configured for Google Maps.
         */
        @JvmStatic
        @JvmOverloads
        public fun googleMaps(googleMaps: GoogleMaps = GoogleMaps()): Tool {
            @OptIn(PublicPreviewAPI::class)
            return Tool(null, null, null, null, null, googleMaps)
        }
    }
}
