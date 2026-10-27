/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.messaging

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.FirebaseException
import io.github.corneloaie.firebase.UnsupportedOnJs
import io.github.corneloaie.firebase.externals.jsGet
import io.github.corneloaie.firebase.externals.toKotlin
import io.github.corneloaie.firebase.externals.toKotlinString
import io.github.corneloaie.firebase.messaging.externals.Messaging
import io.github.corneloaie.firebase.messaging.externals.deleteToken
import io.github.corneloaie.firebase.messaging.externals.getMessaging
import io.github.corneloaie.firebase.messaging.externals.getToken
import kotlin.js.JsException
import kotlin.js.Promise

/** @property js The underlying Firebase JS SDK object. */
public actual class FirebaseMessaging internal constructor(public val js: Messaging) {
    public actual fun deleteToken(): Task<Nothing?> = task({ deleteToken(js) }) { null }

    public actual fun getToken(): Task<String> = task({ getToken(js) }) { it.toKotlinString() }

    /** A no-op: the JS SDK has no client-side topic subscription (a web token is subscribed from a server with the Admin SDK). */
    @UnsupportedOnJs
    public actual fun subscribeToTopic(topic: String): Task<Nothing?> = completedTask()

    /** A no-op: the JS SDK has no client-side topic subscription (a web token is subscribed from a server with the Admin SDK). */
    @UnsupportedOnJs
    public actual fun unsubscribeFromTopic(topic: String): Task<Nothing?> = completedTask()

    /** Always false: the JS SDK only generates a token in [getToken]. Setting it does nothing. */
    @UnsupportedOnJs
    public actual var isAutoInitEnabled: Boolean
        get() = false
        set(_) = Unit

    /** Fails: the JS SDK has no registration by Firebase installation ID. */
    @UnsupportedOnJs
    public actual fun register(): Task<Nothing?> = unsupportedOnJs("FirebaseMessaging.register")

    /** Fails: the JS SDK has no registration by Firebase installation ID. */
    @UnsupportedOnJs
    public actual fun unregister(): Task<Nothing?> = unsupportedOnJs("FirebaseMessaging.unregister")

    /** Always false: notification delegation is an Android feature. */
    @UnsupportedOnJs
    public actual fun isNotificationDelegationEnabled(): Boolean = false

    /** Does nothing: notification delegation is an Android feature. */
    @UnsupportedOnJs
    public actual fun setNotificationDelegationEnabled(enable: Boolean): Task<Nothing?> = completedTask()

    /** Always false: the JS SDK exports delivery data through its own configuration. */
    @UnsupportedOnJs
    public actual fun deliveryMetricsExportToBigQueryEnabled(): Boolean = false

    /** Does nothing: the JS SDK exports delivery data through its own configuration. */
    @UnsupportedOnJs
    public actual fun setDeliveryMetricsExportToBigQuery(enable: Boolean) {}

    override fun equals(other: Any?): Boolean = other is FirebaseMessaging && other.js == js

    override fun hashCode(): Int = js.hashCode()

    override fun toString(): String = "FirebaseMessaging($js)"

    public actual companion object {
        @Deprecated("The registration token has no scope any more; getToken() returns the FCM token")
        public actual val INSTANCE_ID_SCOPE: String = "FCM"

        public actual fun getInstance(): FirebaseMessaging = rethrow { FirebaseMessaging(getMessaging()) }
    }
}

private fun completedTask(): Task<Nothing?> = TaskCompletionSource<Nothing?>().apply { setResult(null) }.task

private fun <T> unsupportedOnJs(member: String): Task<T> = TaskCompletionSource<T>().apply { setException(UnsupportedOperationException("$member is not provided by the Firebase JS SDK")) }.task

private inline fun <T : JsAny?, R> task(start: () -> Promise<T>, crossinline result: (T) -> R): Task<R> {
    val source = TaskCompletionSource<R>()
    try {
        start().then(
            onFulfilled = {
                source.setResult(result(it))
                null
            },
            onRejected = {
                source.setException(it.toFirebaseException(null))
                null
            },
        )
    } catch (e: Throwable) {
        source.setException(e.toFirebaseException())
    }
    return source.task
}

internal inline fun <R> rethrow(function: () -> R): R = try {
    function()
} catch (e: Throwable) {
    throw e.toFirebaseException()
}

private fun Throwable.toFirebaseException(): FirebaseException = when (this) {
    is FirebaseException -> this
    is JsException -> thrownValue?.toFirebaseException(this) ?: FirebaseException(message ?: "Unknown error", this)
    else -> FirebaseException(message ?: "Unknown error", this)
}

/** A promise rejection carries the JS error itself rather than a [Throwable]. */
private fun JsAny.toFirebaseException(cause: Throwable?): FirebaseException {
    val message = jsGet(this, "message").toKotlin()?.toString() ?: jsGet(this, "code").toKotlin()?.toString() ?: "Unknown error"
    return FirebaseException(message, cause ?: toThrowableOrNull() ?: Exception(message))
}
