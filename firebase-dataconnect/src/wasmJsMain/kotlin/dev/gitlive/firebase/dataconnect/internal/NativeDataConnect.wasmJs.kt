/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.dataconnect.internal

import com.google.firebase.FirebaseApp
import com.google.firebase.dataconnect.ConnectorConfig
import com.google.firebase.dataconnect.DataConnectException
import com.google.firebase.dataconnect.DataConnectSettings
import com.google.firebase.dataconnect.DataSource
import com.google.firebase.dataconnect.QueryRef
import dev.gitlive.firebase.dataconnect.externals.DataConnect
import dev.gitlive.firebase.dataconnect.externals.connectDataConnectEmulator
import dev.gitlive.firebase.dataconnect.externals.executeMutation
import dev.gitlive.firebase.dataconnect.externals.executeQuery
import dev.gitlive.firebase.dataconnect.externals.getDataConnect
import dev.gitlive.firebase.dataconnect.externals.makeMemoryCacheProvider
import dev.gitlive.firebase.dataconnect.externals.mutationRef
import dev.gitlive.firebase.dataconnect.externals.queryRef
import dev.gitlive.firebase.dataconnect.externals.terminate
import dev.gitlive.firebase.externals.jsGet
import dev.gitlive.firebase.externals.json
import dev.gitlive.firebase.externals.jsonParse
import dev.gitlive.firebase.externals.jsonStringify
import dev.gitlive.firebase.externals.toKotlin
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsException
import kotlin.js.Promise

/**
 * The Firebase JS SDK's `firebase/data-connect`. Its instances are cached per app and connector like the Android SDK's,
 * and [close] terminates the instance so that a later `getInstance` creates a new one with possibly different settings.
 * The JS SDK has no host/SSL settings (it always calls the production service unless the emulator is connected), and its
 * only cache storage is memory, which `CacheSettings.Storage.PERSISTENT` maps to as well.
 */
internal actual class NativeDataConnect actual constructor(app: FirebaseApp, config: ConnectorConfig, settings: DataConnectSettings) {

    /** Without cache settings the SDK gets an empty settings object: Kotlin/Wasm would pass `null`, not `undefined`. */
    private val js: DataConnect = getDataConnect(
        app.js,
        json("location" to config.location, "connector" to config.connector, "service" to config.serviceId),
        settings.cacheSettings?.let { cache ->
            json("cacheSettings" to json("cacheProvider" to makeMemoryCacheProvider(), "maxAgeSeconds" to cache.maxAgeSeconds))
        } ?: json(),
    )

    actual fun useEmulator(host: String, port: Int) {
        jsCall { connectDataConnectEmulator(js, host, port, false) }
    }

    actual suspend fun executeQuery(operationName: String, variables: JsonObject, fetchPolicy: QueryRef.FetchPolicy): NativeQueryResult {
        val result = jsCall { executeQuery(queryRef(js, operationName, variables.toJsObject()), json("fetchPolicy" to fetchPolicy.name)) }.awaitResult()
        return NativeQueryResult(result.data.toJsonValue(), if (result.source == "CACHE") DataSource.CACHE else DataSource.SERVER)
    }

    actual suspend fun executeMutation(operationName: String, variables: JsonObject): JsonElement = jsCall { executeMutation(mutationRef(js, operationName, variables.toJsObject())) }.awaitResult().data.toJsonValue()

    actual fun close() {
        terminate(js)
    }
}

/** The plain JS object of a JSON object, as the SDK expects variables. */
private fun JsonObject.toJsObject(): JsAny = jsonParse(toString())!!

/**
 * The JSON of a value the SDK returned (`undefined` reaches Kotlin/Wasm as `null`). Not named `toJsonElement`, the
 * conversion of Kotlin values in JsonCodec.kt, which would take a JS value it is handed by mistake and fail on it.
 */
private fun JsAny?.toJsonValue(): JsonElement = if (this == null) JsonNull else Json.parseToJsonElement(jsonStringify(this))

/** Runs an SDK call, rethrowing its errors as this module's exceptions. */
private inline fun <T> jsCall(block: () -> T): T = try {
    block()
} catch (e: Throwable) {
    throw e.toDataConnectFailure()
}

/**
 * Awaits a promise of the SDK. kotlinx.coroutines' `await` only recovers exceptions thrown from Kotlin, so a rejection
 * with the SDK's `DataConnectError` would lose its `code` and `response`: it is mapped as a synchronous failure is.
 */
private suspend fun <T : JsAny?> Promise<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    then(
        onFulfilled = {
            continuation.resume(it)
            null
        },
        onRejected = {
            continuation.resumeWithException(it.toDataConnectFailure(null))
            null
        },
    )
}

/** A JS error reaches Kotlin/Wasm as a [JsException]; an exception thrown from Kotlin is rethrown as it is. */
private fun Throwable.toDataConnectFailure(): Throwable = (this as? JsException)?.thrownValue?.toDataConnectFailure(this) ?: this

/**
 * A `DataConnectOperationError` (the operation ran but the server reported errors) becomes [NativeOperationFailure] with
 * its `response`; any other `DataConnectError` becomes `DataConnectException`. [thrown] is the [JsException] the error was
 * thrown as; a promise rejection carries the JS error itself, which becomes the exception it would be thrown as.
 */
private fun JsAny.toDataConnectFailure(thrown: Throwable?): Throwable {
    val response = jsGet(this, "response")
    val message = jsGet(this, "message")?.toKotlin() as? String
    val cause = thrown ?: toThrowableOrNull() ?: Exception(message ?: toString())
    return when {
        response != null -> NativeOperationFailure(
            message = message ?: cause.toString(),
            rawData = jsGet(response, "data").toJsonValue().takeIf { it !is JsonNull },
            errors = parseErrorInfos(jsGet(response, "errors").toJsonValue()),
            cause = cause,
        )
        jsGet(this, "code") != null -> DataConnectException(message ?: cause.toString(), cause)
        else -> cause
    }
}
