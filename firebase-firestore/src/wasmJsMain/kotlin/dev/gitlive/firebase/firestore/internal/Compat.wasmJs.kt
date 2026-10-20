/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.firestore.internal

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.objectKeys
import com.google.firebase.firestore.rethrow
import com.google.firebase.firestore.toFirestoreException
import dev.gitlive.firebase.externals.jsGet
import dev.gitlive.firebase.externals.toKotlin
import dev.gitlive.firebase.internal.EncodedObject
import dev.gitlive.firebase.internal.js
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.Promise

/** The JS primitives become Kotlin's; objects and arrays stay JS values, and the layer's value types the Kotlin objects. */
internal actual fun EncodedObject.toCompatMap(): Map<String, Any?> = js.let { json -> objectKeys(json).associateWith { jsGet(json, it).toKotlin() } }

internal actual fun DocumentSnapshot.nativeData(serverTimestampBehavior: DocumentSnapshot.ServerTimestampBehavior): Any? = nativeData(serverTimestampBehavior)

internal actual fun DocumentSnapshot.nativeGet(field: String, serverTimestampBehavior: DocumentSnapshot.ServerTimestampBehavior): Any? = nativeGet(field, serverTimestampBehavior)

internal actual fun DocumentSnapshot.nativeGet(fieldPath: FieldPath, serverTimestampBehavior: DocumentSnapshot.ServerTimestampBehavior): Any? = nativeGet(fieldPath, serverTimestampBehavior)

/** The result of the body, a Kotlin value, is passed through the JS SDK's promise as a reference to the Kotlin object. */
@OptIn(DelicateCoroutinesApi::class)
internal actual suspend fun <T> FirebaseFirestore.runSuspendTransaction(updateFunction: suspend (Transaction) -> T): T = rethrow {
    val result = runPromiseTransaction<JsAny?>(null) { transaction -> GlobalScope.promise { (updateFunction(transaction) as Any?)?.toJsReference() } }.awaitFirestore()
    @Suppress("UNCHECKED_CAST", "UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
    (result as? JsReference<*>)?.get() as T
}

internal actual suspend fun Transaction.getAwait(documentRef: DocumentReference): DocumentSnapshot = rethrow { DocumentSnapshot(js.get(documentRef.js).awaitFirestore()) }

internal actual fun Query.wrapperSnapshotListener(metadataChanges: MetadataChanges, listener: EventListener<QuerySnapshot>): ListenerRegistration = addSnapshotListener(metadataChanges, listener)

internal actual fun DocumentReference.wrapperSnapshotListener(metadataChanges: MetadataChanges, listener: EventListener<DocumentSnapshot>): ListenerRegistration = addSnapshotListener(metadataChanges, listener)

/**
 * Awaits a promise of the JS SDK. kotlinx.coroutines' `await` only recovers exceptions thrown from Kotlin, so a rejection
 * with the SDK's `FirestoreError` would lose its code: it is mapped as the compatibility layer maps it.
 */
private suspend fun <T : JsAny?> Promise<T>.awaitFirestore(): T = suspendCancellableCoroutine { continuation ->
    then(
        onFulfilled = {
            continuation.resume(it)
            null
        },
        onRejected = {
            continuation.resumeWithException(it.toFirestoreException())
            null
        },
    )
}
