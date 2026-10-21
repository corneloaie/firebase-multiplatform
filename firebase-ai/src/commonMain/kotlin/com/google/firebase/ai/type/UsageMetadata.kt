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

/**
 * Usage metadata about response(s).
 *
 * @param promptTokenCount Number of tokens in the request.
 * @param candidatesTokenCount Number of tokens in the response(s).
 * @param totalTokenCount Total number of tokens.
 * @param promptTokensDetails The breakdown, by modality, of how many tokens are consumed by the
 * prompt.
 * @param candidatesTokensDetails The breakdown, by modality, of how many tokens are consumed by the
 * candidates.
 * @param toolUsePromptTokensDetails The breakdown, by modality, of how many tokens are consumed by
 * tools.
 * @param thoughtsTokenCount The number of tokens used by the model's internal "thinking" process.
 * @param toolUsePromptTokenCount The number of tokens used by tools.
 */
public class UsageMetadata
internal constructor(
  public val promptTokenCount: Int,
  public val candidatesTokenCount: Int?,
  public val totalTokenCount: Int,
  public val cachedContentTokenCount: Int,
  public val promptTokensDetails: List<ModalityTokenCount>,
  public val candidatesTokensDetails: List<ModalityTokenCount>,
  public val cacheTokensDetails: List<ModalityTokenCount>,
  public val thoughtsTokenCount: Int,
  public val toolUsePromptTokenCount: Int,
  public val toolUsePromptTokensDetails: List<ModalityTokenCount>
)
