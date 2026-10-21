/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.ai

import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.FunctionCallPart
import com.google.firebase.ai.type.FunctionResponsePart
import com.google.firebase.ai.type.LiveGenerationConfig
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.SessionResumptionConfig
import com.google.firebase.ai.type.Tool
import dev.gitlive.firebase.ai.internal.NativeAI
import dev.gitlive.firebase.ai.internal.NativeLiveSession
import dev.gitlive.firebase.ai.internal.NativeRequestOptions
import dev.gitlive.firebase.ai.internal.catchAsync
import dev.gitlive.firebase.ai.internal.executeAutoFunction
import dev.gitlive.firebase.ai.internal.liveSetup

@PublicPreviewAPI
public actual class LiveGenerativeModel internal constructor(
    private val native: NativeAI,
    private val modelName: String,
    private val config: LiveGenerationConfig?,
    private val tools: List<Tool>,
    private val systemInstruction: Content?,
    private val requestOptions: RequestOptions,
) {
    private suspend fun open(sessionResumption: SessionResumptionConfig?): NativeLiveSession = native.connect(modelName, liveSetup(config, tools, systemInstruction, sessionResumption), NativeRequestOptions(requestOptions.timeout))

    public actual suspend fun connect(sessionResumption: SessionResumptionConfig?): LiveSession = catchAsync {
        LiveSession(open(sessionResumption), ::open, ::hasFunction, ::executeFunction)
    }

    internal fun hasFunction(call: FunctionCallPart): Boolean = tools.flatMap { it.autoFunctionDeclarations.orEmpty() }.any { it.name == call.name && it.functionReference != null }

    internal suspend fun executeFunction(call: FunctionCallPart): FunctionResponsePart {
        if (tools.isEmpty()) throw RuntimeException("No registered tools")
        val declaration = tools.flatMap { it.autoFunctionDeclarations.orEmpty() }.firstOrNull { it.name == call.name }
            ?: throw RuntimeException("No registered function named ${call.name}")
        return executeAutoFunction(call, declaration.name, declaration.inputSchema, declaration.outputSchema, declaration.functionReference)
    }
}
