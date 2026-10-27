/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

@file:JvmName("AIKt")

package io.github.corneloaie.firebase.ai

import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.GenerativeBackend
import io.github.corneloaie.firebase.Firebase
import io.github.corneloaie.firebase.FirebaseApp
import kotlin.jvm.JvmName
import com.google.firebase.ai.ai as compatAi

/*
 * The Kotlin-first layer of AI Logic is deliberately thin: the com.google.firebase.ai API is already built on coroutines,
 * Flow and kotlinx.serialization, so it is used as is from common code. Only the entry points of the Kotlin-first
 * `Firebase` object are added here; they go through the `Firebase.ai` extensions (FirebaseAIKt), which are real static
 * methods on every platform, rather than the companion object of the header stub.
 */

/** The [FirebaseAI] instance for the default app, using the Gemini Developer API backend. */
public val Firebase.ai: FirebaseAI
    get() = com.google.firebase.Firebase.compatAi

/**
 * The [FirebaseAI] instance for [app] and [backend].
 *
 * @param backend the backend to send the requests to: [GenerativeBackend.googleAI] (the Gemini Developer API) or
 *   [GenerativeBackend.agentPlatform].
 * @param useLimitedUseAppCheckTokens whether to send App Check's limited-use tokens instead of the cached ones.
 */
public fun Firebase.ai(
    app: FirebaseApp,
    backend: GenerativeBackend = GenerativeBackend.googleAI(),
    useLimitedUseAppCheckTokens: Boolean = false,
): FirebaseAI = com.google.firebase.Firebase.compatAi(app.compat, backend, useLimitedUseAppCheckTokens)
