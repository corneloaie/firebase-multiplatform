/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.database

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import dev.gitlive.firebase.Unsubscribe
import dev.gitlive.firebase.UnsupportedOnJs
import dev.gitlive.firebase.database.externals.Database
import dev.gitlive.firebase.database.externals.QueryConstraint
import dev.gitlive.firebase.database.externals.onChildAdded
import dev.gitlive.firebase.database.externals.onChildChanged
import dev.gitlive.firebase.database.externals.onChildMoved
import dev.gitlive.firebase.database.externals.onChildRemoved
import dev.gitlive.firebase.database.externals.onValue
import dev.gitlive.firebase.database.externals.query
import dev.gitlive.firebase.externals.jsGet
import dev.gitlive.firebase.externals.jsObject
import dev.gitlive.firebase.externals.json
import dev.gitlive.firebase.externals.toJs
import dev.gitlive.firebase.externals.toKotlin
import kotlin.js.JsException
import kotlin.js.Promise
import dev.gitlive.firebase.database.externals.DatabaseReference as JsDatabaseReference
import dev.gitlive.firebase.database.externals.child as jsChild
import dev.gitlive.firebase.database.externals.get as jsGetValue
import dev.gitlive.firebase.database.externals.getDatabase as jsGetDatabase
import dev.gitlive.firebase.database.externals.goOffline as jsGoOffline
import dev.gitlive.firebase.database.externals.goOnline as jsGoOnline
import dev.gitlive.firebase.database.externals.onDisconnect as jsOnDisconnect
import dev.gitlive.firebase.database.externals.push as jsPush
import dev.gitlive.firebase.database.externals.remove as jsRemove
import dev.gitlive.firebase.database.externals.set as jsSetValue
import dev.gitlive.firebase.database.externals.setPriority as jsSetPriority
import dev.gitlive.firebase.database.externals.setWithPriority as jsSetWithPriority
import dev.gitlive.firebase.database.externals.update as jsUpdate
import dev.gitlive.firebase.database.externals.OnDisconnect as JsOnDisconnect
import dev.gitlive.firebase.database.externals.Query as JsQuery
import dev.gitlive.firebase.database.externals.endAt as jsEndAt
import dev.gitlive.firebase.database.externals.endBefore as jsEndBefore
import dev.gitlive.firebase.database.externals.equalTo as jsEqualTo
import dev.gitlive.firebase.database.externals.limitToFirst as jsLimitToFirst
import dev.gitlive.firebase.database.externals.limitToLast as jsLimitToLast
import dev.gitlive.firebase.database.externals.orderByChild as jsOrderByChild
import dev.gitlive.firebase.database.externals.orderByKey as jsOrderByKey
import dev.gitlive.firebase.database.externals.orderByPriority as jsOrderByPriority
import dev.gitlive.firebase.database.externals.orderByValue as jsOrderByValue
import dev.gitlive.firebase.database.externals.runTransactionWithJsUpdate as jsRunTransaction
import dev.gitlive.firebase.database.externals.startAfter as jsStartAfter
import dev.gitlive.firebase.database.externals.startAt as jsStartAt

/**
 * @property js The underlying Firebase JS SDK object.
 * @property jsDatabase The JS SDK database the query belongs to (the JS SDK does not expose it on the query).
 */
public actual open class Query internal constructor(public open val js: JsQuery, public val jsDatabase: Database) {
    public actual val ref: DatabaseReference get() = DatabaseReference(js.ref, jsDatabase)

    public actual fun addValueEventListener(listener: ValueEventListener): ValueEventListener {
        val unsubscribe = rethrow {
            onValue(js, { listener.onDataChange(DataSnapshot(it, jsDatabase)) }, { listener.onCancelled(it.toDatabaseError()) })
        }
        Subscriptions.add(listener, unsubscribe)
        return listener
    }

    public actual fun addChildEventListener(listener: ChildEventListener): ChildEventListener {
        val cancel: (JsAny) -> Unit = { listener.onCancelled(it.toDatabaseError()) }
        rethrow {
            Subscriptions.add(listener, onChildAdded(js, { snapshot, previous -> listener.onChildAdded(DataSnapshot(snapshot, jsDatabase), previous) }, cancel))
            Subscriptions.add(listener, onChildChanged(js, { snapshot, previous -> listener.onChildChanged(DataSnapshot(snapshot, jsDatabase), previous) }, cancel))
            Subscriptions.add(listener, onChildMoved(js, { snapshot, previous -> listener.onChildMoved(DataSnapshot(snapshot, jsDatabase), previous) }, cancel))
            Subscriptions.add(listener, onChildRemoved(js, { snapshot, _ -> listener.onChildRemoved(DataSnapshot(snapshot, jsDatabase)) }, cancel))
        }
        return listener
    }

    public actual fun addListenerForSingleValueEvent(listener: ValueEventListener) {
        rethrow {
            onValue(js, { listener.onDataChange(DataSnapshot(it, jsDatabase)) }, { listener.onCancelled(it.toDatabaseError()) }, json("onlyOnce" to true))
        }
    }

    public actual fun removeEventListener(listener: ValueEventListener) {
        Subscriptions.remove(listener)
    }

    public actual fun removeEventListener(listener: ChildEventListener) {
        Subscriptions.remove(listener)
    }

    public actual fun get(): Task<DataSnapshot> = task({ jsGetValue(js) }) { DataSnapshot(it, jsDatabase) }

    /** A no-op: the JS SDK has no persistent cache to keep a query synchronized in. */
    @UnsupportedOnJs
    public actual fun keepSynced(keepSynced: Boolean): Unit = Unit

    private fun constrained(constraint: QueryConstraint): Query = rethrow { Query(query(js, constraint), jsDatabase) }

    public actual fun orderByChild(path: String): Query = constrained(jsOrderByChild(path))
    public actual fun orderByKey(): Query = constrained(jsOrderByKey())
    public actual fun orderByPriority(): Query = constrained(jsOrderByPriority())
    public actual fun orderByValue(): Query = constrained(jsOrderByValue())
    public actual fun limitToFirst(limit: Int): Query = constrained(jsLimitToFirst(limit))
    public actual fun limitToLast(limit: Int): Query = constrained(jsLimitToLast(limit))
    public actual fun startAt(value: String?): Query = constrained(jsStartAt(value?.toJsString()))
    public actual fun startAt(value: String?, key: String?): Query = constrained(startAtConstraint(value?.toJsString(), key))
    public actual fun startAt(value: Double): Query = constrained(jsStartAt(value.toJsNumber()))
    public actual fun startAt(value: Double, key: String?): Query = constrained(startAtConstraint(value.toJsNumber(), key))
    public actual fun startAt(value: Boolean): Query = constrained(jsStartAt(value.toJsBoolean()))
    public actual fun startAt(value: Boolean, key: String?): Query = constrained(startAtConstraint(value.toJsBoolean(), key))
    public actual fun startAfter(value: String?): Query = constrained(jsStartAfter(value?.toJsString()))
    public actual fun startAfter(value: String?, key: String?): Query = constrained(startAfterConstraint(value?.toJsString(), key))
    public actual fun startAfter(value: Double): Query = constrained(jsStartAfter(value.toJsNumber()))
    public actual fun startAfter(value: Double, key: String?): Query = constrained(startAfterConstraint(value.toJsNumber(), key))
    public actual fun startAfter(value: Boolean): Query = constrained(jsStartAfter(value.toJsBoolean()))
    public actual fun startAfter(value: Boolean, key: String?): Query = constrained(startAfterConstraint(value.toJsBoolean(), key))
    public actual fun endAt(value: String?): Query = constrained(jsEndAt(value?.toJsString()))
    public actual fun endAt(value: String?, key: String?): Query = constrained(endAtConstraint(value?.toJsString(), key))
    public actual fun endAt(value: Double): Query = constrained(jsEndAt(value.toJsNumber()))
    public actual fun endAt(value: Double, key: String?): Query = constrained(endAtConstraint(value.toJsNumber(), key))
    public actual fun endAt(value: Boolean): Query = constrained(jsEndAt(value.toJsBoolean()))
    public actual fun endAt(value: Boolean, key: String?): Query = constrained(endAtConstraint(value.toJsBoolean(), key))
    public actual fun endBefore(value: String?): Query = constrained(jsEndBefore(value?.toJsString()))
    public actual fun endBefore(value: String?, key: String?): Query = constrained(endBeforeConstraint(value?.toJsString(), key))
    public actual fun endBefore(value: Double): Query = constrained(jsEndBefore(value.toJsNumber()))
    public actual fun endBefore(value: Double, key: String?): Query = constrained(endBeforeConstraint(value.toJsNumber(), key))
    public actual fun endBefore(value: Boolean): Query = constrained(jsEndBefore(value.toJsBoolean()))
    public actual fun endBefore(value: Boolean, key: String?): Query = constrained(endBeforeConstraint(value.toJsBoolean(), key))
    public actual fun equalTo(value: String?): Query = constrained(jsEqualTo(value?.toJsString()))
    public actual fun equalTo(value: String?, key: String?): Query = constrained(equalToConstraint(value?.toJsString(), key))
    public actual fun equalTo(value: Double): Query = constrained(jsEqualTo(value.toJsNumber()))
    public actual fun equalTo(value: Double, key: String?): Query = constrained(equalToConstraint(value.toJsNumber(), key))
    public actual fun equalTo(value: Boolean): Query = constrained(jsEqualTo(value.toJsBoolean()))
    public actual fun equalTo(value: Boolean, key: String?): Query = constrained(equalToConstraint(value.toJsBoolean(), key))

    /** The JS SDK creates a new object per call, so queries are equal when they are of the same location and constraints. */
    override fun equals(other: Any?): Boolean = other is Query && (other.js == js || js.isEqual(other.js))

    override fun hashCode(): Int = js.toString().hashCode()

    override fun toString(): String = js.toString()
}

/*
 * The JS SDK validates a key only when it is not `undefined`, and rejects a null one. Kotlin/Wasm has no `undefined`
 * (a null argument reaches JS as `null`), so an absent key omits the argument, which the externals' `definedExternally`
 * default passes as `undefined`.
 */
private fun startAtConstraint(value: JsAny?, key: String?): QueryConstraint = if (key == null) jsStartAt(value) else jsStartAt(value, key)
private fun startAfterConstraint(value: JsAny?, key: String?): QueryConstraint = if (key == null) jsStartAfter(value) else jsStartAfter(value, key)
private fun endAtConstraint(value: JsAny?, key: String?): QueryConstraint = if (key == null) jsEndAt(value) else jsEndAt(value, key)
private fun endBeforeConstraint(value: JsAny?, key: String?): QueryConstraint = if (key == null) jsEndBefore(value) else jsEndBefore(value, key)
private fun equalToConstraint(value: JsAny?, key: String?): QueryConstraint = if (key == null) jsEqualTo(value) else jsEqualTo(value, key)

/** The JS SDK unsubscribe functions of each listener, so that a listener can be removed the Android way. */
private object Subscriptions {
    private val unsubscribes = mutableMapOf<Any, MutableList<Unsubscribe>>()

    fun add(listener: Any, unsubscribe: Unsubscribe) {
        unsubscribes.getOrPut(listener) { mutableListOf() } += unsubscribe
    }

    fun remove(listener: Any) {
        unsubscribes.remove(listener)?.forEach { rethrow { it() } }
    }
}

/** @property js The underlying Firebase JS SDK object. */
public actual open class DatabaseReference internal constructor(override val js: JsDatabaseReference, jsDatabase: Database) : Query(js, jsDatabase) {
    public actual val database: FirebaseDatabase get() = FirebaseDatabase(jsDatabase)
    public actual val key: String? get() = js.key
    public actual val parent: DatabaseReference? get() = js.parent?.let { DatabaseReference(it, jsDatabase) }
    public actual val root: DatabaseReference get() = DatabaseReference(js.root, jsDatabase)

    public actual fun child(pathString: String): DatabaseReference = rethrow { DatabaseReference(jsChild(js, pathString), jsDatabase) }

    public actual fun push(): DatabaseReference = rethrow { DatabaseReference(jsPush(js), jsDatabase) }

    public actual fun onDisconnect(): OnDisconnect = rethrow { OnDisconnect(jsOnDisconnect(js), this) }

    public actual fun setValue(value: Any?): Task<Nothing?> = write { jsSetValue(js, value.toJs()) }

    public actual fun setValue(value: Any?, listener: CompletionListener?) {
        write { jsSetValue(js, value.toJs()) }.notify(listener, this)
    }

    public actual fun setValue(value: Any?, priority: Any?): Task<Nothing?> = write { jsSetWithPriority(js, value.toJs(), priority.toJs()) }

    public actual fun setValue(value: Any?, priority: Any?, listener: CompletionListener?) {
        write { jsSetWithPriority(js, value.toJs(), priority.toJs()) }.notify(listener, this)
    }

    public actual fun setPriority(priority: Any?): Task<Nothing?> = write { jsSetPriority(js, priority.toJs()) }

    public actual fun setPriority(priority: Any?, listener: CompletionListener?) {
        write { jsSetPriority(js, priority.toJs()) }.notify(listener, this)
    }

    public actual fun updateChildren(update: Map<String, Any?>): Task<Nothing?> = write { jsUpdate(js, update.toJs()!!) }

    public actual fun updateChildren(update: Map<String, Any?>, listener: CompletionListener?) {
        write { jsUpdate(js, update.toJs()!!) }.notify(listener, this)
    }

    public actual fun removeValue(): Task<Nothing?> = write { jsRemove(js) }

    public actual fun removeValue(listener: CompletionListener?) {
        write { jsRemove(js) }.notify(listener, this)
    }

    public actual fun runTransaction(handler: Transaction.Handler) {
        runTransaction(handler, true)
    }

    public actual fun runTransaction(handler: Transaction.Handler, fireLocalEvents: Boolean) {
        val transaction = try {
            val abort = jsObject()
            jsRunTransaction(
                js,
                transactionUpdate = abortableUpdate(abort) { current ->
                    val data = MutableData(current, key)
                    if (handler.doTransaction(data).isSuccess) data.root.js else abort
                },
                options = json("applyLocally" to fireLocalEvents),
            )
        } catch (e: Throwable) {
            handler.onComplete(e.toDatabaseError(), false, null)
            return
        }
        transaction.then(
            onFulfilled = { result ->
                handler.onComplete(null, result.committed, DataSnapshot(result.snapshot, jsDatabase))
                null
            },
            onRejected = { error ->
                handler.onComplete(error.toDatabaseError(), false, null)
                null
            },
        )
    }

    public actual fun interface CompletionListener {
        public actual fun onComplete(error: DatabaseError?, ref: DatabaseReference)
    }

    public actual companion object {
        /** The JS SDK disconnects per database: this disconnects the default app's. */
        public actual fun goOffline() {
            rethrow { jsGoOffline(jsGetDatabase()) }
        }

        /** The JS SDK reconnects per database: this reconnects the default app's. */
        public actual fun goOnline() {
            rethrow { jsGoOnline(jsGetDatabase()) }
        }
    }
}

/**
 * The transaction update the JS SDK calls: it runs [update] and returns `undefined`, which aborts the transaction, when
 * [update] returns [abort]. A Kotlin/Wasm lambda cannot return `undefined` itself (its `null` reaches JS as `null`, which
 * deletes the data).
 */
private fun abortableUpdate(abort: JsAny, update: (current: JsAny?) -> JsAny?): JsAny = js("(current) => { const next = update(current); return next === abort ? undefined : next; }")

/**
 * @property js The underlying Firebase JS SDK object.
 * @property ref The reference the operations are registered on.
 */
public actual class OnDisconnect internal constructor(public val js: JsOnDisconnect, public val ref: DatabaseReference) {
    public actual fun setValue(value: Any?): Task<Nothing?> = write { js.set(value.toJs()) }

    public actual fun setValue(value: Any?, listener: DatabaseReference.CompletionListener?) {
        write { js.set(value.toJs()) }.notify(listener, ref)
    }

    public actual fun setValue(value: Any?, priority: String?): Task<Nothing?> = write { js.setWithPriority(value.toJs(), priority?.toJsString()) }

    public actual fun setValue(value: Any?, priority: String?, listener: DatabaseReference.CompletionListener?) {
        write { js.setWithPriority(value.toJs(), priority?.toJsString()) }.notify(listener, ref)
    }

    public actual fun setValue(value: Any?, priority: Double): Task<Nothing?> = write { js.setWithPriority(value.toJs(), priority.toJsNumber()) }

    public actual fun setValue(value: Any?, priority: Double, listener: DatabaseReference.CompletionListener?) {
        write { js.setWithPriority(value.toJs(), priority.toJsNumber()) }.notify(listener, ref)
    }

    public actual fun setValue(value: Any?, priority: Map<*, *>?, listener: DatabaseReference.CompletionListener?) {
        write { js.setWithPriority(value.toJs(), priority.toJs()) }.notify(listener, ref)
    }

    public actual fun updateChildren(update: Map<String, Any?>): Task<Nothing?> = write { js.update(update.toJs()!!) }

    public actual fun updateChildren(update: Map<String, Any?>, listener: DatabaseReference.CompletionListener?) {
        write { js.update(update.toJs()!!) }.notify(listener, ref)
    }

    public actual fun removeValue(): Task<Nothing?> = write { js.remove() }

    public actual fun removeValue(listener: DatabaseReference.CompletionListener?) {
        write { js.remove() }.notify(listener, ref)
    }

    public actual fun cancel(): Task<Nothing?> = write { js.cancel() }

    public actual fun cancel(listener: DatabaseReference.CompletionListener) {
        write { js.cancel() }.notify(listener, ref)
    }
}

/** Calls [listener] with the outcome of this write. */
private fun Task<Nothing?>.notify(listener: DatabaseReference.CompletionListener?, ref: DatabaseReference) {
    if (listener == null) return
    addOnCompleteListener { task -> listener.onComplete(task.exception?.toDatabaseError(), ref) }
}

private inline fun write(start: () -> Promise<JsAny?>): Task<Nothing?> = task(start) { null }

internal inline fun <T : JsAny?, R> task(start: () -> Promise<T>, crossinline result: (T) -> R): Task<R> {
    val source = TaskCompletionSource<R>()
    try {
        start().then(
            onFulfilled = {
                source.setResult(result(it))
                null
            },
            onRejected = {
                source.setException(it.toDatabaseException())
                null
            },
        )
    } catch (e: Throwable) {
        source.setException(e.toDatabaseException())
    }
    return source.task
}

internal inline fun <R> rethrow(function: () -> R): R = try {
    function()
} catch (e: Throwable) {
    throw e.toDatabaseException()
}

/** The JS SDK reports failures as errors whose message starts with the Android SDK's code name (`PERMISSION_DENIED: ...`). */
internal fun Throwable.toDatabaseError(): DatabaseError {
    if (this is DatabaseException && cause != null && cause !is DatabaseException) return cause!!.toDatabaseError()
    val message = ((this as? JsException)?.thrownValue?.errorText() ?: message ?: toString()).removePrefix(DATABASE_ERROR_PREFIX)
    val codeName = message.substringBefore(':').trim().takeIf { name -> name.isNotEmpty() && name.all { it == '_' || it.isUpperCase() } }
    return DatabaseError.of(codeName, message)
}

internal fun Throwable.toDatabaseException(): DatabaseException = this as? DatabaseException ?: DatabaseException("$DATABASE_ERROR_PREFIX${toDatabaseError().message}", this)

/** A promise rejection or a cancelled listener carries the JS error itself rather than a [Throwable]. */
internal fun JsAny.toDatabaseError(): DatabaseError = toThrowable().toDatabaseError()

internal fun JsAny.toDatabaseException(): DatabaseException = toThrowable().toDatabaseException()

/** The JS error as Kotlin/JS sees it: a Kotlin exception that went through JS as itself, any other JS value as a [JsException]. */
private fun JsAny.toThrowable(): Throwable = toThrowableOrNull() ?: Exception(errorText())

/** A JS error's text as Kotlin/JS reads it: its `message`, else its `code`, else the value as a string. */
private fun JsAny.errorText(): String = jsGet(this, "message")?.toKotlin()?.toString() ?: jsGet(this, "code")?.toKotlin()?.toString() ?: toString()

private const val DATABASE_ERROR_PREFIX = "Firebase Database error: "
