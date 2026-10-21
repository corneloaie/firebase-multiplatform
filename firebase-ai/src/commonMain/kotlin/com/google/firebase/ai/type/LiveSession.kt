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

import kotlinx.coroutines.flow.Flow

/** Represents a live WebSocket session capable of streaming content to and from the server. */
@PublicPreviewAPI
public expect class LiveSession {

    /**
     * Starts an audio conversation with the model, which can only be stopped using
     * [stopAudioConversation] or [close].
     *
     * @param functionCallHandler A callback function that is invoked whenever the model receives a
     * function call. The [FunctionResponsePart] that the callback function returns will be
     * automatically sent to the model.
     */
    public suspend fun startAudioConversation(
        functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)? = null,
    )

    /**
     * Starts an audio conversation with the model, which can only be stopped using
     * [stopAudioConversation] or [close].
     *
     * @param functionCallHandler A callback function that is invoked whenever the model receives a
     * function call. The [FunctionResponsePart] that the callback function returns will be
     * automatically sent to the model.
     *
     * @param enableInterruptions If enabled, allows the user to speak over or interrupt the model's
     * ongoing reply.
     *
     * **WARNING**: The user interruption feature relies on device-specific support, and may not be
     * consistently available.
     */
    public suspend fun startAudioConversation(
        functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)? = null,
        enableInterruptions: Boolean = false,
    )

    /**
     * Starts an audio conversation with the model, which can only be stopped using
     * [stopAudioConversation] or [close].
     *
     * @param functionCallHandler A callback function that is invoked whenever the model receives a
     * function call. The [FunctionResponsePart] that the callback function returns will be
     * automatically sent to the model.
     *
     * @param transcriptHandler A callback function that is invoked whenever the model receives a
     * transcript. The first [Transcription] object is the input transcription, and the second is the
     * output transcription.
     *
     * @param enableInterruptions If enabled, allows the user to speak over or interrupt the model's
     * ongoing reply.
     *
     * **WARNING**: The user interruption feature relies on device-specific support, and may not be
     * consistently available.
     */
    public suspend fun startAudioConversation(
        functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)? = null,
        transcriptHandler: ((Transcription?, Transcription?) -> Unit)? = null,
        enableInterruptions: Boolean = false,
    )

    /**
     * Starts an audio conversation with the model, which can only be stopped using
     * [stopAudioConversation] or [close].
     *
     * @param functionCallHandler A callback function that is invoked whenever the model receives a
     * function call. The [FunctionResponsePart] that the callback function returns will be
     * automatically sent to the model.
     *
     * @param transcriptHandler A callback function that is invoked whenever the model receives a
     * transcript. The first [Transcription] object is the input transcription, and the second is the
     * output transcription.
     *
     * @param goAwayHandler A callback function that is invoked when the server initiates a disconnect
     * via a [LiveServerGoAway] message. This allows the application to handle server-initiated
     * session termination gracefully.
     *
     * @param enableInterruptions If enabled, allows the user to speak over or interrupt the model's
     * ongoing reply.
     *
     * **WARNING**: The user interruption feature relies on device-specific support, and may not be
     * consistently available.
     */
    public suspend fun startAudioConversation(
        functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)? = null,
        transcriptHandler: ((Transcription?, Transcription?) -> Unit)? = null,
        goAwayHandler: ((LiveServerGoAway) -> Unit)? = null,
        enableInterruptions: Boolean = false,
    )

    /**
     * Starts an audio conversation with the model, which can only be stopped using
     * [stopAudioConversation] or [close].
     *
     * @param liveAudioConversationConfig A [LiveAudioConversationConfig] provided by the user to
     * control the various aspects of the conversation.
     */
    public suspend fun startAudioConversation(liveAudioConversationConfig: LiveAudioConversationConfig)

    /**
     * Stops the audio conversation with the model.
     *
     * This only needs to be called after a previous call to [startAudioConversation].
     *
     * If there is no audio conversation currently active, this function does nothing.
     */
    public fun stopAudioConversation()

    /** Indicates whether the underlying websocket connection is active. */
    public fun isClosed(): Boolean

    /** Indicates whether an audio conversation is being used for this session object. */
    public fun isAudioConversationActive(): Boolean

    /**
     * Receives responses from the model for both streaming and standard requests.
     *
     * Call [close] to stop receiving responses from the model.
     *
     * @return A [Flow] which will emit [LiveServerMessage] from the model.
     *
     * @throws [SessionAlreadyReceivingException] when the session is already receiving.
     * @see stopReceiving
     */
    public fun receive(): Flow<LiveServerMessage>

    /**
     * Stops receiving from the model.
     *
     * If this function is called during an ongoing audio conversation, the model's response will not
     * be received, and no audio will be played; the live session object will no longer receive data
     * from the server.
     *
     * To resume receiving data, you must either handle it directly using [receive], or indirectly by
     * using [startAudioConversation].
     *
     * @see close
     */
    public fun stopReceiving()

    /**
     * Sends function calling responses to the model.
     *
     * **NOTE:** If you're using [startAudioConversation], the method will handle sending function
     * responses to the model for you. You do _not_ need to call this method in that case.
     *
     * @param functionList The list of [FunctionResponsePart] instances indicating the function
     * response from the client.
     */
    public suspend fun sendFunctionResponse(functionList: List<FunctionResponsePart>)

    /**
     * Sends an audio input stream to the model, using the realtime API.
     *
     * To learn more about audio formats, and the required state they should be provided in, see the
     * docs on
     * [Supported audio formats](https://cloud.google.com/vertex-ai/generative-ai/docs/live-api#supported-audio-formats)
     *
     * @param audio Raw audio data used to update the model on the client's conversation. For best
     * results, send 16-bit PCM audio at 24kHz.
     */
    public suspend fun sendAudioRealtime(audio: InlineData)

    /**
     * Sends a video frame to the model, using the realtime API.
     *
     * Instead of raw video data, the model expects individual frames of the video, sent as images.
     *
     * If your video has audio, send it separately through [sendAudioRealtime].
     *
     * For better performance, frames can also be sent at a lower rate than the video; even as low as
     * 1 frame per second.
     *
     * @param video Encoded image data extracted from a frame of the video, used to update the model
     * on the client's conversation, with the corresponding IANA standard MIME type of the video frame
     * data (for example, `image/png`, `image/jpeg`, and so on).
     */
    public suspend fun sendVideoRealtime(video: InlineData)

    /**
     * Sends a text input stream to the model, using the realtime API.
     *
     * @param text Text content to append to the current client's conversation.
     */
    public suspend fun sendTextRealtime(text: String)

    /**
     * Manually marks the start of user activity, using the realtime API.
     *
     * The start of user activity is effectively the start of a user's turn, but depending on the
     * configuration defined in [RealtimeInputConfig], it may not be interpreted as an interruption.
     * An example of the start of user activity could be the user speaking (not silence).
     *
     * Should be followed with a call to [sendStopActivityRealtime] after all the data has been sent
     * for the user's turn.
     *
     * Only required when automatic activity detection is disabled via [RealtimeInputConfig].
     */
    public suspend fun sendStartActivityRealtime()

    /**
     * Manually marks the end of user activity, using the realtime API.
     *
     * The end of user activity is effectively the end of a user's turn, and signals that the model
     * should respond.
     *
     * Should be preceded by a call to [sendStartActivityRealtime].
     *
     * Only required when automatic activity detection is disabled via [RealtimeInputConfig].
     */
    public suspend fun sendStopActivityRealtime()

    /**
     * Streams client data to the model.
     *
     * Calling this after [startAudioConversation] will play the response audio immediately.
     *
     * @param content Client [Content] to be sent to the model.
     * @param turnComplete Whether the client has finished sending its turn.
     */
    public suspend fun send(content: Content, turnComplete: Boolean = true)

    /**
     * Streams text data to the model.
     *
     * Calling this after [startAudioConversation] will play the response audio immediately.
     *
     * @param text Text to be sent to the model.
     * @param turnComplete Whether the client has finished sending its turn.
     */
    public suspend fun send(text: String, turnComplete: Boolean = true)

    /**
     * Closes the client session.
     *
     * Once a [LiveSession] is closed, it can not be reopened; you'll need to start a new
     * [LiveSession].
     *
     * @see stopReceiving
     */
    public suspend fun close()

    /**
     * Resumes an existing live session with the server.
     *
     * This closes the current WebSocket connection and establishes a new one using the same
     * configuration as the original session.
     *
     * @param sessionResumption The configuration for session resumption, such as the handle to the
     * previous session state to restore.
     */
    public suspend fun resumeSession(sessionResumption: SessionResumptionConfig? = null)
}
