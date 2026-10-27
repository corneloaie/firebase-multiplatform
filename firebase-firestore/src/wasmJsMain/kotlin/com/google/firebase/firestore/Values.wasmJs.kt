/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.firestore

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.Timestamp
import io.github.corneloaie.firebase.externals.asJsAny
import io.github.corneloaie.firebase.externals.asJsArray
import io.github.corneloaie.firebase.externals.errorCode
import io.github.corneloaie.firebase.externals.jsGet
import io.github.corneloaie.firebase.externals.jsIsArray
import io.github.corneloaie.firebase.externals.jsObject
import io.github.corneloaie.firebase.externals.jsObjectKeys
import io.github.corneloaie.firebase.externals.jsSet
import io.github.corneloaie.firebase.externals.jsTypeOf
import io.github.corneloaie.firebase.externals.json
import io.github.corneloaie.firebase.externals.toKotlin
import io.github.corneloaie.firebase.externals.toKotlinString
import io.github.corneloaie.firebase.externals.toList
import io.github.corneloaie.firebase.firestore.externals.Bytes
import io.github.corneloaie.firebase.firestore.externals.QueryConstraint
import io.github.corneloaie.firebase.firestore.externals.deleteField
import io.github.corneloaie.firebase.firestore.externals.documentId
import kotlin.js.JsException
import kotlin.js.Promise
import kotlin.math.abs
import kotlin.math.floor
import io.github.corneloaie.firebase.firestore.externals.BsonObjectId as JsBsonObjectId
import io.github.corneloaie.firebase.firestore.externals.BsonTimestamp as JsBsonTimestamp
import io.github.corneloaie.firebase.firestore.externals.Decimal128Value as JsDecimal128Value
import io.github.corneloaie.firebase.firestore.externals.DocumentReference as JsDocumentReference
import io.github.corneloaie.firebase.firestore.externals.FieldPath as JsFieldPath
import io.github.corneloaie.firebase.firestore.externals.GeoPoint as JsGeoPoint
import io.github.corneloaie.firebase.firestore.externals.Int32Value as JsInt32Value
import io.github.corneloaie.firebase.firestore.externals.MaxKey as JsMaxKey
import io.github.corneloaie.firebase.firestore.externals.MinKey as JsMinKey
import io.github.corneloaie.firebase.firestore.externals.RegexValue as JsRegexValue
import io.github.corneloaie.firebase.firestore.externals.Timestamp as JsTimestamp
import io.github.corneloaie.firebase.firestore.externals.VectorValue as JsVectorValue
import io.github.corneloaie.firebase.firestore.externals.and as jsAnd
import io.github.corneloaie.firebase.firestore.externals.arrayRemove as jsArrayRemove
import io.github.corneloaie.firebase.firestore.externals.arrayUnion as jsArrayUnion
import io.github.corneloaie.firebase.firestore.externals.increment as jsIncrement
import io.github.corneloaie.firebase.firestore.externals.maximum as jsMaximum
import io.github.corneloaie.firebase.firestore.externals.minimum as jsMinimum
import io.github.corneloaie.firebase.firestore.externals.or as jsOr
import io.github.corneloaie.firebase.firestore.externals.serverTimestamp as jsServerTimestamp
import io.github.corneloaie.firebase.firestore.externals.vector as jsVector
import io.github.corneloaie.firebase.firestore.externals.where as jsWhere

/*
 * The JS SDK reads and writes plain JS values with its own Timestamp, GeoPoint, DocumentReference, Bytes (with a BSON
 * binary subtype), VectorValue and BSON classes; the layer presents them as the Android SDK's types: Map, List, String,
 * Boolean, Long (integral numbers), Double, com.google.firebase.Timestamp, GeoPoint, DocumentReference, Blob,
 * VectorValue, BsonObjectId, BsonTimestamp, Decimal128Value, Int32Value, MaxKey, MinKey and RegexValue.
 *
 * Kotlin/Wasm values are not JS values: the Kotlin-first layer's encoders keep the layer's value types inside the JS
 * objects and arrays they build as references to the Kotlin objects (see `toJsReference`), which read back as `Any` are
 * the Kotlin objects again. So the Kotlin types are checked before a JS object or array is converted, at every level.
 */

private const val MAX_EXACT_INTEGER = 9007199254740992.0

/** [this] as the JS SDK writes it: Kotlin maps become objects, collections arrays, longs numbers and the layer's value types the SDK's. */
internal fun Any?.toJs(): JsAny? = when (this) {
    null -> null
    is String -> toJsString()
    is Boolean -> toJsBoolean()
    is Number -> toDouble().toJsNumber()
    is Timestamp -> JsTimestamp(seconds.toDouble(), nanoseconds.toDouble())
    is GeoPoint -> JsGeoPoint(latitude, longitude)
    is DocumentReference -> js
    is FieldValue -> toJsFieldValue()
    is Blob -> Bytes.fromUint8Array(toBytes().toUint8Array(), subType)
    is VectorValue -> jsVector(toArray().map { it.toJsNumber() }.toJsArray())
    is BsonObjectId -> JsBsonObjectId(value)
    is BsonTimestamp -> JsBsonTimestamp(seconds.toDouble(), increment.toDouble())
    is Decimal128Value -> JsDecimal128Value(stringValue)
    is Int32Value -> JsInt32Value(value)
    is MaxKey -> JsMaxKey.instance()
    is MinKey -> JsMinKey.instance()
    is RegexValue -> JsRegexValue(pattern, options)
    is FieldPath -> toJs()
    is Map<*, *> -> jsObject().also { json -> forEach { (key, value) -> jsSet(json, key.toString(), value.toJs()) } }
    is Collection<*> -> map { it.toJs() }.toJsArray()
    is Array<*> -> map { it.toJs() }.toJsArray()
    else -> asJsAny().let { value ->
        when {
            // The JS SDK rejects an object of a custom class, but would read the reference to a Kotlin object as an empty map.
            isKotlinReference(value) -> throw FirebaseFirestoreException("Unsupported field value: a custom ${this::class.simpleName} object", FirebaseFirestoreException.Code.INVALID_ARGUMENT)
            jsIsArray(value) -> value.asJsArray().toList().map { it.toJs() }.toJsArray()
            isPlainObject(value) -> jsObject().also { json -> objectKeys(value).forEach { key -> jsSet(json, key, jsGet(value, key).toJs()) } }
            else -> value
        }
    }
}

/** A value read by the JS SDK as the Android SDK's types (see [toJs]). */
internal fun JsAny?.toCompat(): Any? {
    if (this == null) return null
    return when (jsTypeOf(this)) {
        "string", "boolean" -> toKotlin()
        "number" -> toCompatNumber()
        "object" -> toCompatValueType() ?: when {
            jsIsArray(this) -> asJsArray().toList().map { it.toCompat() }
            else -> objectKeys(this).associateWith { key -> jsGet(this, key).toCompat() }
        }
        else -> this
    }
}

/** An object of one of the JS SDK's value classes as the layer's value type, or null for any other value. */
private fun JsAny.toCompatValueType(): Any? = when (this) {
    is JsTimestamp -> Timestamp(seconds.toLong(), nanoseconds.toInt())
    is JsGeoPoint -> GeoPoint(latitude, longitude)
    is JsDocumentReference -> DocumentReference(this)
    is Bytes -> Blob.createBsonBinary(subtype, toUint8Array().toByteArray())
    is JsVectorValue -> VectorValue(toArray().toList().map { it.toDouble() }.toDoubleArray())
    is JsBsonObjectId -> BsonObjectId(value)
    is JsBsonTimestamp -> BsonTimestamp(seconds.toLong(), increment.toLong())
    is JsDecimal128Value -> Decimal128Value(stringValue)
    is JsInt32Value -> Int32Value(value)
    is JsMaxKey -> MaxKey.instance()
    is JsMinKey -> MinKey.instance()
    is JsRegexValue -> RegexValue(pattern, options)
    else -> null
}

/**
 * A value read by the JS SDK with only the leaves converted to the layer's value types, objects and arrays kept as the
 * JS SDK's; what the Kotlin-first layer's decoders take. Inside objects and arrays the leaves are references to the Kotlin
 * objects, which the decoders read back as the Kotlin objects; at the top level JS primitives are Kotlin's, as the
 * decoders' elements are.
 */
internal fun JsAny?.toCompatLeaves(): Any? = this?.compatLeaves().toKotlin()

private fun JsAny.compatLeaves(): JsAny = toCompatValueType()?.toJsReference() ?: when {
    jsIsArray(this) -> asJsArray().toList().map { it?.compatLeaves() }.toJsArray()
    isPlainObject(this) -> jsObject().also { json -> objectKeys(this).forEach { key -> jsSet(json, key, jsGet(this, key)?.compatLeaves()) } }
    else -> this
}

/** A JS number as the Android SDK reads it: a [Long] when integral, a [Double] otherwise. */
internal fun JsAny.toCompatNumber(): Number {
    val number = jsNumberToDouble(this)
    return if (number == floor(number) && abs(number) < MAX_EXACT_INTEGER) number.toLong() else number
}

private fun FieldValue.toJsFieldValue(): JsAny = when (operation) {
    FieldValue.Operation.SERVER_TIMESTAMP -> jsServerTimestamp()
    FieldValue.Operation.DELETE -> deleteField()
    FieldValue.Operation.ARRAY_UNION -> jsArrayUnion(*arguments.map { it.toJs()!! }.toTypedArray())
    FieldValue.Operation.ARRAY_REMOVE -> jsArrayRemove(*arguments.map { it.toJs()!! }.toTypedArray())
    FieldValue.Operation.INCREMENT -> jsIncrement((arguments.single() as Number).toDouble())
    FieldValue.Operation.MINIMUM -> jsMinimum((arguments.single() as Number).toDouble())
    FieldValue.Operation.MAXIMUM -> jsMaximum((arguments.single() as Number).toDouble())
}

internal fun FieldPath.toJs(): JsFieldPath = if (isDocumentId) documentId() else JsFieldPath(*segments.toTypedArray())

internal fun SetOptions.toJs(): JsAny = if (merge) json("merge" to true) else json("mergeFields" to mergeFields.orEmpty().map { it.toJs() })

internal fun Filter.Node.toJs(): QueryConstraint = when (this) {
    is Filter.Node.Field -> jsWhere(fieldPath.toJs(), operator.jsOperator, value.toJs())
    is Filter.Node.Composite -> when (operator) {
        Filter.Operator.AND -> jsAnd(*filters.map { it.toJs() }.toTypedArray())
        else -> jsOr(*filters.map { it.toJs() }.toTypedArray())
    }
}

/** The bytes as a JS `Uint8Array`, copied one by one as Kotlin/Wasm arrays are not JS arrays. */
internal fun ByteArray.toUint8Array(): JsAny = newUint8Array(size).also { array -> forEachIndexed { index, byte -> setArrayElement(array, index, byte.toInt()) } }

/** The bytes of a JS `Uint8Array` (this). */
internal fun JsAny.toByteArray(): ByteArray = ByteArray(arrayLength(this)) { getArrayElement(this, it).toByte() }

/** The `ArrayBuffer` of a JS `Uint8Array`. */
internal fun arrayBuffer(array: JsAny): JsAny = js("array.buffer")

private fun newUint8Array(length: Int): JsAny = js("new Uint8Array(length)")

private fun arrayLength(array: JsAny): Int = js("array.length")

private fun getArrayElement(array: JsAny, index: Int): Int = js("array[index]")

private fun setArrayElement(array: JsAny, index: Int, value: Int) {
    js("array[index] = value")
}

private fun jsNumberToDouble(value: JsAny): Double = js("value")

internal fun isPlainObject(value: JsAny): Boolean = js("typeof value === 'object' && value !== null && !Array.isArray(value) && (Object.getPrototypeOf(value) === null || Object.getPrototypeOf(value) === Object.prototype)")

/** Whether [value] is a Kotlin object passed to JS: as a WebAssembly GC object it has a null prototype and is not extensible. */
private fun isKotlinReference(value: JsAny): Boolean = js("typeof value === 'object' && value !== null && Object.getPrototypeOf(value) === null && !Object.isExtensible(value)")

internal fun objectKeys(value: JsAny): List<String> = jsObjectKeys(value).toList().map { it.toKotlinString() }

/** The JS SDK's `FirestoreError` (with a `code` such as `permission-denied`) as the Android SDK's exception. */
internal fun Throwable.toFirestoreException(): FirebaseFirestoreException = when (this) {
    is FirebaseFirestoreException -> this
    is JsException -> firestoreException(errorCode(), thrownValue?.let { jsGet(it, "message") }.toKotlin()?.toString() ?: message, this)
    else -> firestoreException(null, message, this)
}

/**
 * A promise rejection or a listener error carries the JS error itself rather than a [Throwable]. A Kotlin exception
 * that reached JS from a callback, such as a failed transaction body, is the Kotlin exception again.
 */
internal fun JsAny.toFirestoreException(): FirebaseFirestoreException = toThrowableOrNull()?.toFirestoreException()
    ?: jsGet(this, "message").toKotlin()?.toString().let { message -> firestoreException(jsGet(this, "code").toKotlin()?.toString(), message, Exception(message)) }

private fun firestoreException(jsCode: String?, message: String?, cause: Throwable): FirebaseFirestoreException {
    val code = jsCode?.uppercase()?.replace('-', '_')
    return FirebaseFirestoreException(
        message ?: code ?: cause.toString(),
        FirebaseFirestoreException.Code.entries.firstOrNull { it.name == code } ?: FirebaseFirestoreException.Code.UNKNOWN,
        cause,
    )
}

/** The JS SDK's promise as a [Task]; [result] converts its value, a rejection or a synchronous throw fails the task. */
internal inline fun <T : JsAny?, R> task(start: () -> Promise<T>, crossinline result: (T) -> R): Task<R> {
    val source = TaskCompletionSource<R>()
    try {
        start().then(
            onFulfilled = {
                try {
                    source.setResult(result(it))
                } catch (e: Throwable) {
                    source.setException(e.toFirestoreException())
                }
                null
            },
            onRejected = {
                source.setException(it.toFirestoreException())
                null
            },
        )
    } catch (e: Throwable) {
        source.setException(e.toFirestoreException())
    }
    return source.task
}

internal inline fun write(start: () -> Promise<JsAny?>): Task<Nothing?> = task(start) { null }

internal inline fun <R> rethrow(function: () -> R): R = try {
    function()
} catch (e: Throwable) {
    throw e.toFirestoreException()
}

internal actual fun isIntegerValue(number: Number): Boolean = number is Byte || number is Short || number is Int || number is Long

internal actual fun isSameFirestore(a: FirebaseFirestore, b: FirebaseFirestore): Boolean = a === b || a.js === b.js
