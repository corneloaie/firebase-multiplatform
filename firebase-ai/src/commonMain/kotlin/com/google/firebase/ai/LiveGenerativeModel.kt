/*
 * Copyright 2025 Google LLC
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

package com.google.firebase.ai

import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ServiceConnectionHandshakeFailedException
import com.google.firebase.ai.type.SessionResumptionConfig

/**
 * Represents a multimodal model (like Gemini) capable of real-time content generation based on
 * various input types, supporting bidirectional streaming.
 */
@PublicPreviewAPI
public expect class LiveGenerativeModel {
    /**
     * Start a [LiveSession] with the server for bidirectional streaming.
     *
     * @return A [LiveSession] that you can use to stream messages to and from the server.
     * @throws [ServiceConnectionHandshakeFailedException] If the client was not able to establish a
     * connection with the server.
     */
    public suspend fun connect(sessionResumption: SessionResumptionConfig? = null): LiveSession
}
