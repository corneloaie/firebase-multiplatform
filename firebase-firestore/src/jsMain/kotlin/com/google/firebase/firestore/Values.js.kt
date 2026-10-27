/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.firestore

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.Timestamp
import io.github.corneloaie.firebase.firestore.externals.Bytes
import io.github.corneloaie.firebase.firestore.externals.QueryConstraint
import io.github.corneloaie.firebase.firestore.externals.deleteField
import io.github.corneloaie.firebase.firestore.externals.documentId
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json
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
 */

private const val MAX_EXACT_INTEGER = 9007199254740992.0

/** [this] as the JS SDK writes it: Kotlin maps become objects, collections arrays, longs numbers and the layer's value types the SDK's. */
internal fun Any?.toJs(): Any? = when (this) {
    null -> null
    is String, is Boolean -> this
    is Long -> toDouble()
    is Number -> this
    is Timestamp -> JsTimestamp(seconds.toDouble(), nanoseconds.toDouble())
    is GeoPoint -> JsGeoPoint(latitude, longitude)
    is DocumentReference -> js
    is FieldValue -> toJsFieldValue()
    is Blob -> Bytes.fromUint8Array(toBytes().toUint8Array(), subType)
    is VectorValue -> jsVector(toArray().toTypedArray())
    is BsonObjectId -> JsBsonObjectId(value)
    is BsonTimestamp -> JsBsonTimestamp(seconds.toDouble(), increment.toDouble())
    is Decimal128Value -> JsDecimal128Value(stringValue)
    is Int32Value -> JsInt32Value(value)
    is MaxKey -> JsMaxKey.instance()
    is MinKey -> JsMinKey.instance()
    is RegexValue -> JsRegexValue(pattern, options)
    is FieldPath -> toJs()
    is Map<*, *> -> json(*entries.map { (key, value) -> key.toString() to value.toJs() }.toTypedArray())
    is Collection<*> -> map { it.toJs() }.toTypedArray()
    is Array<*> -> map { it.toJs() }.toTypedArray()
    else -> if (isPlainObject(this)) json(*objectKeys(this).map { key -> key to asDynamic()[key].unsafeCast<Any?>().toJs() }.toTypedArray()) else this
}

/** A value read by the JS SDK as the Android SDK's types (see [toJs]). */
internal fun Any?.toCompat(): Any? {
    if (this == null || this == undefined) return null
    return when (jsTypeOf(this)) {
        "string", "boolean" -> this
        "number" -> toCompatNumber()
        "object" -> toCompatValueType() ?: when {
            isArray(this) -> unsafeCast<Array<Any?>>().map { it.toCompat() }
            else -> objectKeys(this).associateWith { key -> asDynamic()[key].unsafeCast<Any?>().toCompat() }
        }
        else -> this
    }
}

/**
 * A value read by the JS SDK with only the leaves converted to the layer's value types, objects and arrays kept as the
 * JS SDK's; what the Kotlin-first layer's decoders take.
 */
internal fun Any?.toCompatLeaves(): Any? {
    if (this == null || this == undefined) return null
    return toCompatValueType() ?: when {
        isArray(this) -> unsafeCast<Array<Any?>>().map { it.toCompatLeaves() }.toTypedArray()
        isPlainObject(this) -> json(*objectKeys(this).map { key -> key to asDynamic()[key].unsafeCast<Any?>().toCompatLeaves() }.toTypedArray())
        else -> this
    }
}

/** An object of one of the JS SDK's value classes as the layer's value type, or null for any other value. */
private fun Any.toCompatValueType(): Any? = when (this) {
    is JsTimestamp -> Timestamp(seconds.toLong(), nanoseconds.toInt())
    is JsGeoPoint -> GeoPoint(latitude, longitude)
    is JsDocumentReference -> DocumentReference(this)
    is Bytes -> Blob.createBsonBinary(subtype, toUint8Array().toByteArray())
    is JsVectorValue -> VectorValue(toArray().toDoubleArray())
    is JsBsonObjectId -> BsonObjectId(value)
    is JsBsonTimestamp -> BsonTimestamp(seconds.toLong(), increment.toLong())
    is JsDecimal128Value -> Decimal128Value(stringValue)
    is JsInt32Value -> Int32Value(value)
    is JsMaxKey -> MaxKey.instance()
    is JsMinKey -> MinKey.instance()
    is JsRegexValue -> RegexValue(pattern, options)
    else -> null
}

/** A JS number as the Android SDK reads it: a [Long] when integral, a [Double] otherwise. */
internal fun Any.toCompatNumber(): Number {
    val number = unsafeCast<Double>()
    return if (number == floor(number) && abs(number) < MAX_EXACT_INTEGER) number.toLong() else number
}

private fun FieldValue.toJsFieldValue(): Any = when (operation) {
    FieldValue.Operation.SERVER_TIMESTAMP -> jsServerTimestamp()
    FieldValue.Operation.DELETE -> deleteField()
    FieldValue.Operation.ARRAY_UNION -> jsArrayUnion(*arguments.map { it.toJs()!! }.toTypedArray())
    FieldValue.Operation.ARRAY_REMOVE -> jsArrayRemove(*arguments.map { it.toJs()!! }.toTypedArray())
    FieldValue.Operation.INCREMENT -> jsIncrement((arguments.single() as Number).toDouble())
    FieldValue.Operation.MINIMUM -> jsMinimum((arguments.single() as Number).toDouble())
    FieldValue.Operation.MAXIMUM -> jsMaximum((arguments.single() as Number).toDouble())
}

internal fun FieldPath.toJs(): JsFieldPath = if (isDocumentId) documentId() else JsFieldPath(*segments.toTypedArray())

internal fun SetOptions.toJs(): Json = if (merge) json("merge" to true) else json("mergeFields" to mergeFields.orEmpty().map { it.toJs() }.toTypedArray())

internal fun Filter.Node.toJs(): QueryConstraint = when (this) {
    is Filter.Node.Field -> jsWhere(fieldPath.toJs(), operator.jsOperator, value.toJs())
    is Filter.Node.Composite -> when (operator) {
        Filter.Operator.AND -> jsAnd(*filters.map { it.toJs() }.toTypedArray())
        else -> jsOr(*filters.map { it.toJs() }.toTypedArray())
    }
}

internal fun ByteArray.toUint8Array(): Uint8Array = Uint8Array(toTypedArray())

internal fun Uint8Array.toByteArray(): ByteArray = ByteArray(length) { this[it] }

internal fun isArray(value: Any?): Boolean = js("Array.isArray")(value).unsafeCast<Boolean>()

internal fun isPlainObject(value: Any?): Boolean {
    if (value == null || jsTypeOf(value) != "object" || isArray(value)) return false
    val prototype: dynamic = js("Object.getPrototypeOf")(value)
    return prototype == null || prototype === js("Object.prototype")
}

internal fun objectKeys(value: Any?): Array<String> = js("Object.keys")(value).unsafeCast<Array<String>>()

/** The JS SDK's `FirestoreError` (with a `code` such as `permission-denied`) as the Android SDK's exception. */
internal fun Throwable.toFirestoreException(): FirebaseFirestoreException {
    if (this is FirebaseFirestoreException) return this
    val code = asDynamic().code.unsafeCast<String?>()?.uppercase()?.replace('-', '_')
    return FirebaseFirestoreException(
        message ?: code ?: toString(),
        FirebaseFirestoreException.Code.entries.firstOrNull { it.name == code } ?: FirebaseFirestoreException.Code.UNKNOWN,
        this,
    )
}

internal inline fun <T> task(start: () -> Promise<T>): Task<T> {
    val source = TaskCompletionSource<T>()
    try {
        start().then({ source.setResult(it) }, { source.setException(it.toFirestoreException()) })
    } catch (e: Throwable) {
        source.setException(e.toFirestoreException())
    }
    return source.task
}

internal inline fun write(start: () -> Promise<Unit>): Task<Nothing?> = task { start().then { null } }

internal inline fun <R> rethrow(function: () -> R): R = try {
    function()
} catch (e: Throwable) {
    throw e.toFirestoreException()
}

/** Kotlin/JS numbers other than Long are JS numbers, whose type checks match any number: the JS SDK sends the integral ones as integers. */
internal actual fun isIntegerValue(number: Number): Boolean = number is Long || number.toDouble().let { it == floor(it) && abs(it) < MAX_EXACT_INTEGER }

internal actual fun isSameFirestore(a: FirebaseFirestore, b: FirebaseFirestore): Boolean = a === b || a.js === b.js
