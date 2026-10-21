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

/**
 * The model's response to a count tokens request.
 *
 * **Important:** The counters in this class do not include billable image, video or other non-text
 * input. See [Pricing](https://firebase.google.com/docs/ai-logic/pricing) for details.
 *
 * @property totalTokens The total number of tokens in the input given to the model as a prompt.
 * @property promptTokensDetails The breakdown, by modality, of how many tokens are consumed by the
 * prompt.
 */
public class CountTokensResponse
internal constructor(
  public val totalTokens: Int,
  public val promptTokensDetails: List<ModalityTokenCount> = emptyList(),
) {
  public operator fun component1(): Int = totalTokens

  public operator fun component2(): List<ModalityTokenCount> = promptTokensDetails
}
