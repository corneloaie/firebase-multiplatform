/*
 * Copyright 2026 Google LLC
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
 */

package com.google.firebase.ai.type

import kotlin.jvm.JvmOverloads

/**
 * How the text of a [TextPart] is spoken when the model generates speech from it.
 *
 * @property speaker The speaker of the text, for a multi-speaker conversation.
 * @property style How the text is spoken, for example "cheerfully".
 */
@PublicPreviewAPI
public class SpeechMetadata
@JvmOverloads
constructor(
    public val speaker: String? = null,
    public val style: String? = null,
)
