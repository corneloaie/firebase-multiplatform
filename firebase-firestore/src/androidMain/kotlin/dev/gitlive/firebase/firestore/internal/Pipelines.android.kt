/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.firestore.internal

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Pipeline
import com.google.firebase.firestore.pipeline.InternalOptions

// Unreachable: on Android (and the JVM, which shares this source) the common Pipeline class is a header stub, replaced by
// the SDK's own at runtime.
internal actual fun executePipeline(firestore: FirebaseFirestore, pipeline: Pipeline, options: InternalOptions?): Task<Pipeline.Snapshot> = throw UnsupportedOperationException("The Android SDK's pipeline API is used on Android and the JVM")
