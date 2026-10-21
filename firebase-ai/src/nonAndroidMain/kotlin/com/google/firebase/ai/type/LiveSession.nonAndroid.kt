/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.ai.type

import dev.gitlive.firebase.UnsupportedOnApple
import dev.gitlive.firebase.UnsupportedOnJs
import dev.gitlive.firebase.ai.internal.NativeLiveSession
import dev.gitlive.firebase.ai.internal.catchAsync
import dev.gitlive.firebase.ai.internal.realtimeInput
import dev.gitlive.firebase.ai.internal.toJson
import dev.gitlive.firebase.ai.internal.toLiveServerMessage
import dev.gitlive.firebase.ai.internal.toolResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.selects.select
import kotlinx.serialization.json.JsonArray

/**
 * A Live API session of the platform SDK. The messages of the server are buffered from the moment the session opens
 * until they are [receive]d. [resumeSession] opens a new session of the platform SDK and closes the current one, as
 * the Android SDK does.
 *
 * The platforms other than Android have no audio conversation of the SDK's own (the Android SDK records and plays the
 * audio itself): [startAudioConversation] does nothing there, so stream audio with [sendAudioRealtime] and play the
 * audio parts the server sends.
 */
@PublicPreviewAPI
public actual class LiveSession internal constructor(
    session: NativeLiveSession,
    private val connect: suspend (SessionResumptionConfig?) -> NativeLiveSession,
    private val hasFunction: (FunctionCallPart) -> Boolean,
    private val executeFunction: suspend (FunctionCallPart) -> FunctionResponsePart,
) {
    /** An open session of the platform SDK and the messages its server sent that were not received yet. */
    private class Connection(val native: NativeLiveSession) {
        val messages = Channel<LiveServerMessage>(Channel.UNLIMITED)
        var closed = false
    }

    private var connection: Connection = open(session)

    private var receiving = false
    private var stopSignal = CompletableDeferred<Unit>()

    private fun open(native: NativeLiveSession): Connection = Connection(native).also { connection ->
        native.start(
            onMessage = { message -> connection.messages.trySend(message.toLiveServerMessage()) },
            // As on Android, receiving ends when the connection closes, whatever closed it.
            onClose = {
                connection.closed = true
                connection.messages.close()
            },
        )
    }

    @UnsupportedOnJs
    @UnsupportedOnApple
    public actual suspend fun startAudioConversation(functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)?) {}

    @UnsupportedOnJs
    @UnsupportedOnApple
    public actual suspend fun startAudioConversation(functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)?, enableInterruptions: Boolean) {}

    @UnsupportedOnJs
    @UnsupportedOnApple
    public actual suspend fun startAudioConversation(
        functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)?,
        transcriptHandler: ((Transcription?, Transcription?) -> Unit)?,
        enableInterruptions: Boolean,
    ) {}

    @UnsupportedOnJs
    @UnsupportedOnApple
    public actual suspend fun startAudioConversation(
        functionCallHandler: ((FunctionCallPart) -> FunctionResponsePart)?,
        transcriptHandler: ((Transcription?, Transcription?) -> Unit)?,
        goAwayHandler: ((LiveServerGoAway) -> Unit)?,
        enableInterruptions: Boolean,
    ) {}

    @UnsupportedOnJs
    @UnsupportedOnApple
    public actual suspend fun startAudioConversation(liveAudioConversationConfig: LiveAudioConversationConfig) {}

    /** Stops receiving, as on Android (where it also stops the audio conversation, which does not exist here). */
    public actual fun stopAudioConversation() {
        stopReceiving()
    }

    public actual fun isClosed(): Boolean = connection.closed

    public actual fun isAudioConversationActive(): Boolean = false

    public actual fun receive(): Flow<LiveServerMessage> {
        if (receiving) throw SessionAlreadyReceivingException()
        receiving = true
        val stop = CompletableDeferred<Unit>().also { stopSignal = it }
        return flow {
            while (true) {
                val current = connection
                val next = select<ChannelResult<LiveServerMessage>?> {
                    stop.onAwait { null }
                    current.messages.onReceiveCatching { it }
                } ?: break
                if (next.isClosed) {
                    // A closed session that resumeSession replaced: continue with the new one.
                    if (current === connection) break else continue
                }
                emit(next.getOrThrow())
            }
        }.onCompletion { if (stopSignal === stop) receiving = false }
    }

    public actual fun stopReceiving() {
        if (!receiving) return
        receiving = false
        stopSignal.complete(Unit)
    }

    public actual suspend fun sendFunctionResponse(functionList: List<FunctionResponsePart>): Unit = catchAsync {
        connection.native.sendToolResponse(toolResponse(functionList))
    }

    public actual suspend fun sendAudioRealtime(audio: InlineData): Unit = catchAsync {
        connection.native.sendRealtimeInput(realtimeInput(audio = audio))
    }

    public actual suspend fun sendVideoRealtime(video: InlineData): Unit = catchAsync {
        connection.native.sendRealtimeInput(realtimeInput(video = video))
    }

    public actual suspend fun sendTextRealtime(text: String): Unit = catchAsync {
        connection.native.sendRealtimeInput(realtimeInput(text = text))
    }

    public actual suspend fun sendStartActivityRealtime(): Unit = catchAsync {
        connection.native.sendRealtimeInput(realtimeInput(activityStart = true))
    }

    public actual suspend fun sendStopActivityRealtime(): Unit = catchAsync {
        connection.native.sendRealtimeInput(realtimeInput(activityEnd = true))
    }

    public actual suspend fun send(content: Content, turnComplete: Boolean): Unit = catchAsync {
        connection.native.sendClientContent(JsonArray(listOf(content.toJson())), turnComplete)
    }

    public actual suspend fun send(text: String, turnComplete: Boolean) {
        send(content { text(text) }, turnComplete)
    }

    public actual suspend fun close(): Unit = catchAsync {
        connection.native.close()
        stopReceiving()
    }

    public actual suspend fun resumeSession(sessionResumption: SessionResumptionConfig?): Unit = catchAsync {
        val old = connection
        connection = open(connect(sessionResumption))
        try {
            old.native.close()
        } catch (e: Exception) {
            // The old session is gone either way.
        }
    }
}
