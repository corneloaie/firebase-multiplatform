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

/**
 * Configuration parameters to use for conversation config.
 *
 * @property functionCallHandler A callback that is invoked whenever the model receives a function
 * call. The [FunctionResponsePart] that the callback function returns will be automatically sent to
 * the model.
 *
 * @property transcriptHandler A callback that is invoked whenever the model receives a transcript.
 * The first [Transcription] object is the input transcription, and the second is the output
 * transcription.
 *
 * On Android the configuration also takes an `initializationHandler`, which is invoked with the
 * Android-only `AudioRecord.Builder` and `AudioTrack.Builder` of the audio session, so it is not
 * available in common code (see `api/android-sdk/exclusions.txt`).
 *
 * @property goAwayHandler A callback that is invoked when the server initiates a disconnect via a
 * [LiveServerGoAway] message. This allows the application to handle server-initiated session
 * termination gracefully, such as displaying a message to the user or attempting to reconnect.
 *
 * @property enableInterruptions If enabled, allows the user to speak over or interrupt the model's
 * ongoing reply.
 *
 * **WARNING**: The user interruption feature relies on device-specific support, and may not be
 * consistently available.
 */
@PublicPreviewAPI
public class LiveAudioConversationConfig
private constructor(
  internal val functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)?,
  internal val transcriptHandler: ((Transcription?, Transcription?) -> Unit)?,
  internal val goAwayHandler: ((LiveServerGoAway) -> Unit)?,
  internal val enableInterruptions: Boolean
) {
  /**
   * Builder for creating a [LiveAudioConversationConfig].
   *
   * Mainly intended for Java interop. Kotlin consumers should use [liveAudioConversationConfig] for
   * a more idiomatic experience.
   *
   * @property functionCallHandler See [LiveAudioConversationConfig.functionCallHandler].
   *
   * @property transcriptHandler See [LiveAudioConversationConfig.transcriptHandler].
   *
   * @property goAwayHandler See [LiveAudioConversationConfig.goAwayHandler].
   *
   * @property enableInterruptions See [LiveAudioConversationConfig.enableInterruptions].
   */
  public class Builder {
    @JvmField public var functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)? = null
    @JvmField public var transcriptHandler: ((Transcription?, Transcription?) -> Unit)? = null
    @JvmField public var goAwayHandler: ((LiveServerGoAway) -> Unit)? = null
    @JvmField public var enableInterruptions: Boolean = false

    public fun setFunctionCallHandler(
      functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)?
    ): Builder = apply { this.functionCallHandler = functionCallHandler }

    public fun setTranscriptHandler(
      transcriptHandler: ((Transcription?, Transcription?) -> Unit)?
    ): Builder = apply { this.transcriptHandler = transcriptHandler }

    public fun setGoAwayHandler(goAwayHandler: ((LiveServerGoAway) -> Unit)?): Builder = apply {
      this.goAwayHandler = goAwayHandler
    }

    public fun setEnableInterruptions(enableInterruptions: Boolean): Builder = apply {
      this.enableInterruptions = enableInterruptions
    }

    /** Create a new [LiveAudioConversationConfig] with the attached arguments. */
    public fun build(): LiveAudioConversationConfig =
      LiveAudioConversationConfig(
        functionCallHandler = functionCallHandler,
        transcriptHandler = transcriptHandler,
        goAwayHandler = goAwayHandler,
        enableInterruptions = enableInterruptions
      )
  }

  public companion object {
    /**
     * Alternative casing for [LiveAudioConversationConfig.Builder]:
     * ```
     * val config = LiveAudioConversationConfig.builder()
     * ```
     */
    public fun builder(): Builder = Builder()
  }
}

/**
 * Helper method to construct a [LiveAudioConversationConfig] in a DSL-like manner.
 *
 * Example Usage:
 * ```
 * liveAudioConversationConfig {
 *   functionCallHandler = ...
 *   ...
 * }
 * ```
 */
@OptIn(PublicPreviewAPI::class)
public fun liveAudioConversationConfig(
  init: LiveAudioConversationConfig.Builder.() -> Unit
): LiveAudioConversationConfig {
  val builder = LiveAudioConversationConfig.builder()
  builder.init()
  return builder.build()
}
