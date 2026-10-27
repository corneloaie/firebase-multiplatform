/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.firestore.internal

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Pipeline
import com.google.firebase.firestore.pipeline.InternalOptions

/**
 * Runs [pipeline] on [firestore] with the execute [options] through the platform SDK: the iOS SDK's pipeline bridge on
 * Apple, `firebase/firestore/pipelines` on JS and wasmJs. Android and the JVM never call it: they run the Android SDK's
 * own Pipeline class, which the common one only stands in for at compile time.
 */
internal expect fun executePipeline(firestore: FirebaseFirestore, pipeline: Pipeline, options: InternalOptions?): Task<Pipeline.Snapshot>
