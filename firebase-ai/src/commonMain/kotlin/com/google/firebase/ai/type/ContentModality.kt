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
package com.google.firebase.ai.type

import kotlin.jvm.JvmField


/** Content part modality. */
public class ContentModality private constructor(public val ordinal: Int) {
  public companion object {
    /** Unspecified modality. */
    @JvmField public val UNSPECIFIED: ContentModality = ContentModality(0)

    /** Plain text. */
    @JvmField public val TEXT: ContentModality = ContentModality(1)

    /** Image. */
    @JvmField public val IMAGE: ContentModality = ContentModality(2)

    /** Video. */
    @JvmField public val VIDEO: ContentModality = ContentModality(3)

    /** Audio. */
    @JvmField public val AUDIO: ContentModality = ContentModality(4)

    /** Document (for example, PDF). */
    @JvmField public val DOCUMENT: ContentModality = ContentModality(5)
  }
}
