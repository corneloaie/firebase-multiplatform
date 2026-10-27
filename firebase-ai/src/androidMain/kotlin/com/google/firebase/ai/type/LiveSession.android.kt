/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.ai.type

import io.github.corneloaie.firebase.ai.internal.stub
import kotlinx.coroutines.flow.Flow

// Header stubs of the Android SDK's LiveSession (see utils.stripHeaderStubs).

@PublicPreviewAPI
public actual class LiveSession private constructor() {
    public actual suspend fun startAudioConversation(functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)?): Unit = stub()
    public actual suspend fun startAudioConversation(functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)?, enableInterruptions: Boolean): Unit = stub()
    public actual suspend fun startAudioConversation(
        functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)?,
        transcriptHandler: ((Transcription?, Transcription?) -> Unit)?,
        enableInterruptions: Boolean,
    ): Unit = stub()
    public actual suspend fun startAudioConversation(
        functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)?,
        transcriptHandler: ((Transcription?, Transcription?) -> Unit)?,
        goAwayHandler: ((LiveServerGoAway) -> Unit)?,
        enableInterruptions: Boolean,
    ): Unit = stub()
    public actual suspend fun startAudioConversation(liveAudioConversationConfig: LiveAudioConversationConfig): Unit = stub()
    public actual fun stopAudioConversation(): Unit = stub()
    public actual fun isClosed(): Boolean = stub()
    public actual fun isAudioConversationActive(): Boolean = stub()
    public actual fun receive(): Flow<LiveServerMessage> = stub()
    public actual fun stopReceiving(): Unit = stub()
    public actual suspend fun sendFunctionResponse(functionList: List<FunctionResponsePart>): Unit = stub()
    public actual suspend fun sendAudioRealtime(audio: InlineData): Unit = stub()
    public actual suspend fun sendVideoRealtime(video: InlineData): Unit = stub()
    public actual suspend fun sendTextRealtime(text: String): Unit = stub()
    public actual suspend fun sendStartActivityRealtime(): Unit = stub()
    public actual suspend fun sendStopActivityRealtime(): Unit = stub()

    @JvmOverloads
    public actual suspend fun send(content: Content, turnComplete: Boolean): Unit = stub()

    @JvmOverloads
    public actual suspend fun send(text: String, turnComplete: Boolean): Unit = stub()
    public actual suspend fun close(): Unit = stub()

    @JvmOverloads
    public actual suspend fun resumeSession(sessionResumption: SessionResumptionConfig?): Unit = stub()
}
