/*
 * Copyright 2023 Google LLC
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


/** Category for a given harm rating. */
public class HarmCategory private constructor(public val ordinal: Int) {
  public companion object {
    /** A new and not yet supported value. */
    @JvmField public val UNKNOWN: HarmCategory = HarmCategory(0)

    /** Represents the harm category for content that is classified as harassment. */
    @JvmField public val HARASSMENT: HarmCategory = HarmCategory(1)

    /** Represents the harm category for content that is classified as hate speech. */
    @JvmField public val HATE_SPEECH: HarmCategory = HarmCategory(2)

    /** Represents the harm category for content that is classified as sexually explicit content. */
    @JvmField public val SEXUALLY_EXPLICIT: HarmCategory = HarmCategory(3)

    /** Represents the harm category for content that is classified as dangerous content. */
    @JvmField public val DANGEROUS_CONTENT: HarmCategory = HarmCategory(4)

    /**
     * Represents the harm category for content that is classified as content that may be used to
     * harm civic integrity.
     */
    @JvmField public val CIVIC_INTEGRITY: HarmCategory = HarmCategory(5)

    /** Represents the harm category for image content that is classified as hateful. */
    @JvmField public val IMAGE_HATE: HarmCategory = HarmCategory(6)

    /** Represents the harm category for image content that is classified as dangerous. */
    @JvmField public val IMAGE_DANGEROUS_CONTENT: HarmCategory = HarmCategory(7)

    /** Represents the harm category for image content that is classified as harassment. */
    @JvmField public val IMAGE_HARASSMENT: HarmCategory = HarmCategory(8)

    /** Represents the harm category for image content that is classified as sexually explicit. */
    @JvmField public val IMAGE_SEXUALLY_EXPLICIT: HarmCategory = HarmCategory(9)
  }
}
