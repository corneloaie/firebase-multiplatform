/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.storage

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import io.github.corneloaie.firebase.externals.jsGet
import io.github.corneloaie.firebase.externals.jsObject
import io.github.corneloaie.firebase.externals.jsObjectKeys
import io.github.corneloaie.firebase.externals.jsSet
import io.github.corneloaie.firebase.externals.json
import io.github.corneloaie.firebase.externals.parseDateStringToMillis
import io.github.corneloaie.firebase.externals.toKotlin
import io.github.corneloaie.firebase.externals.toKotlinString
import io.github.corneloaie.firebase.externals.toList
import io.github.corneloaie.firebase.storage.externals.FullMetadata
import io.github.corneloaie.firebase.storage.externals.StorageError
import io.github.corneloaie.firebase.storage.externals.deleteObject
import io.github.corneloaie.firebase.storage.externals.getBytes
import io.github.corneloaie.firebase.storage.externals.getDownloadURL
import io.github.corneloaie.firebase.storage.externals.getMetadata
import io.github.corneloaie.firebase.storage.externals.list
import io.github.corneloaie.firebase.storage.externals.listAll
import io.github.corneloaie.firebase.storage.externals.ref
import io.github.corneloaie.firebase.storage.externals.updateMetadata
import io.github.corneloaie.firebase.storage.externals.uploadBytesResumable
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toInt8Array
import kotlin.js.JsException
import kotlin.js.Promise
import io.github.corneloaie.firebase.storage.externals.ListResult as JsListResult
import io.github.corneloaie.firebase.storage.externals.StorageReference as JsStorageReference
import io.github.corneloaie.firebase.storage.externals.UploadTask as JsUploadTask

/** @property js The underlying Firebase JS SDK object. */
public actual class StorageReference internal constructor(public val js: JsStorageReference) : Comparable<StorageReference> {
    public actual val activeUploadTasks: List<UploadTask> get() = ActiveTasks.uploads.filter { it.reference == this }
    public actual val bucket: String get() = js.bucket
    public actual val name: String get() = js.name
    public actual val parent: StorageReference? get() = js.parent?.let { StorageReference(it) }

    /** With a leading slash, as the Android SDK's `getPath()`. */
    public actual val path: String get() = "/" + js.fullPath

    public actual val root: StorageReference get() = StorageReference(js.root)
    public actual val storage: FirebaseStorage get() = FirebaseStorage(js.storage)

    public actual fun child(pathString: String): StorageReference = rethrow { StorageReference(ref(js, pathString)) }

    actual override fun compareTo(other: StorageReference): Int = toString().compareTo(other.toString())

    public actual fun delete(): Task<Nothing?> = task({ deleteObject(js) }) { null }

    public actual fun getBytes(maxDownloadSizeBytes: Long): Task<ByteArray> = task({ getBytes(js, maxDownloadSizeBytes.toDouble()) }) { Int8Array(it).toByteArray() }

    public actual fun getDownloadUrl(): Task<Any> = task({ getDownloadURL(js) }) { it.toKotlinString() }

    public actual fun getMetadata(): Task<StorageMetadata> = task({ getMetadata(js) }) { it.toCompat() }

    public actual fun list(maxResults: Int): Task<ListResult> = task({ list(js, json("maxResults" to maxResults)) }) { it.toCompat() }

    public actual fun list(maxResults: Int, pageToken: String): Task<ListResult> = task({ list(js, json("maxResults" to maxResults, "pageToken" to pageToken)) }) { it.toCompat() }

    public actual fun listAll(): Task<ListResult> = task({ listAll(js) }) { it.toCompat() }

    public actual fun putBytes(bytes: ByteArray): UploadTask = upload(bytes.toUint8Array(), null)

    public actual fun putBytes(bytes: ByteArray, metadata: StorageMetadata): UploadTask = upload(bytes.toUint8Array(), metadata)

    internal fun upload(data: JsAny?, metadata: StorageMetadata?): UploadTask = rethrow {
        UploadTaskImpl(this, JsUploadController(uploadBytesResumable(js, data, metadata?.toJs())))
    }

    public actual fun updateMetadata(metadata: StorageMetadata): Task<StorageMetadata> = task({ updateMetadata(js, metadata.toJs()) }) { it.toCompat() }

    override fun equals(other: Any?): Boolean = other is StorageReference && other.bucket == bucket && other.path == path

    override fun hashCode(): Int = toString().hashCode()

    override fun toString(): String = "gs://$bucket/$path"
}

/** An [UploadController] over the JS SDK's `UploadTask`. */
internal class JsUploadController(private val js: JsUploadTask) : UploadController {
    override val bytesTransferred: Long get() = js.snapshot.bytesTransferred.toLong()
    override val totalByteCount: Long get() = js.snapshot.totalBytes.toLong()
    override val metadata: StorageMetadata? get() = js.snapshot.metadata?.toCompat()

    override fun start(task: UploadTask) {
        js.on(
            "state_changed",
            { snapshot ->
                when (snapshot.state) {
                    "paused" -> task.reportPaused()
                    "running" -> task.reportProgress()
                    "canceled" -> task.reportCanceled()
                    else -> Unit
                }
            },
            { error -> if (error.code == "storage/canceled") task.reportCanceled() else task.reportFailure(error.toStorageException()) },
            { task.reportSuccess() },
        )
    }

    override fun pause(): Boolean = js.pause()

    override fun resume(): Boolean = js.resume()

    override fun cancel(): Boolean = js.cancel()
}

private inline fun <T : JsAny?, R> task(start: () -> Promise<T>, crossinline result: (T) -> R): Task<R> {
    val source = TaskCompletionSource<R>()
    try {
        start().then(
            onFulfilled = {
                source.setResult(result(it))
                null
            },
            onRejected = {
                source.setException(it.toStorageException(null))
                null
            },
        )
    } catch (e: Throwable) {
        source.setException(e.toStorageException())
    }
    return source.task
}

internal inline fun <R> rethrow(function: () -> R): R = try {
    function()
} catch (e: Throwable) {
    throw e.toStorageException()
}

/** The JS SDK reports `storage/<code>` errors; the codes map onto the Android SDK's `ERROR_*` constants. */
internal fun Throwable.toStorageException(): StorageException = when (this) {
    is StorageException -> this
    is JsException -> thrownValue?.toStorageException(this) ?: toUnknownStorageException()
    else -> toUnknownStorageException()
}

/** A Kotlin exception (or a JS throw of nothing) has no `storage/<code>` and no HTTP status. */
private fun Throwable.toUnknownStorageException(): StorageException = StorageException.of(
    errorCode = StorageException.ERROR_UNKNOWN,
    cause = this,
    message = message ?: "",
)

internal fun StorageError.toStorageException(): StorageException = toStorageException(null)

/** A promise rejection or an upload task's error carries the JS error itself rather than a [Throwable]. */
private fun JsAny.toStorageException(cause: Throwable?): StorageException {
    val code = jsGet(this, "code").toKotlin()?.toString() ?: ""
    val message = jsGet(this, "message").toKotlin()?.toString()
    return StorageException.of(
        errorCode = code.toErrorCode(),
        cause = cause ?: toThrowableOrNull() ?: Exception(message ?: code),
        httpResultCode = (jsGet(this, "status").toKotlin() as? Number)?.toInt() ?: -1,
        message = message ?: code,
    )
}

private fun String.toErrorCode(): Int = when (substringAfter('/')) {
    "object-not-found" -> StorageException.ERROR_OBJECT_NOT_FOUND
    "bucket-not-found" -> StorageException.ERROR_BUCKET_NOT_FOUND
    "project-not-found" -> StorageException.ERROR_PROJECT_NOT_FOUND
    "quota-exceeded" -> StorageException.ERROR_QUOTA_EXCEEDED
    "unauthenticated" -> StorageException.ERROR_NOT_AUTHENTICATED
    "unauthorized" -> StorageException.ERROR_NOT_AUTHORIZED
    "retry-limit-exceeded" -> StorageException.ERROR_RETRY_LIMIT_EXCEEDED
    "invalid-checksum" -> StorageException.ERROR_INVALID_CHECKSUM
    "canceled" -> StorageException.ERROR_CANCELED
    else -> StorageException.ERROR_UNKNOWN
}

/** A copy of the bytes in a JS buffer: a Kotlin/Wasm ByteArray lives in Wasm memory, so it cannot be viewed in place. */
internal fun ByteArray.toUint8Array(): Uint8Array {
    val int8 = toInt8Array()
    return Uint8Array(int8.buffer, int8.byteOffset, int8.length)
}

internal fun JsListResult.toCompat(): ListResult = ListResult(
    items = items.toList().map { StorageReference(it) },
    prefixes = prefixes.toList().map { StorageReference(it) },
    pageToken = nextPageToken.takeUnless { it.isNullOrEmpty() },
)

internal fun StorageMetadata.toJs(): JsAny {
    val custom = jsObject()
    customMetadata.forEach { (key, value) -> jsSet(custom, key, value?.toJsString()) }
    return json(
        "cacheControl" to cacheControl,
        "contentDisposition" to contentDisposition,
        "contentEncoding" to contentEncoding,
        "contentLanguage" to contentLanguage,
        "contentType" to contentType,
        "customMetadata" to custom,
    )
}

internal fun FullMetadata.toCompat(): StorageMetadata = StorageMetadata().also { compat ->
    compat.bucketValue = serverField("bucket")
    compat.cacheControlValue = cacheControl
    compat.contentDispositionValue = contentDisposition
    compat.contentEncodingValue = contentEncoding
    compat.contentLanguageValue = contentLanguage
    compat.contentTypeValue = contentType
    compat.creationTimeMillisValue = serverField("timeCreated").toEpochMillis()
    customMetadata?.let { metadata ->
        jsObjectKeys(metadata).toList().map { it.toKotlinString() }.forEach { key -> compat.customMetadata[key] = jsGet(metadata, key).toKotlin().toString() }
    }
    compat.generationValue = serverField("generation")
    compat.md5HashValue = md5Hash
    compat.metadataGenerationValue = serverField("metageneration")
    compat.nameValue = serverField("name")
    compat.pathValue = serverField("fullPath") ?: ""
    compat.referenceValue = ref?.let { StorageReference(it) }
    compat.sizeBytesValue = size.toLong()
    compat.updatedTimeMillisValue = serverField("updated").toEpochMillis()
}

/**
 * A field the server sets, or null if it is absent. While an upload runs, its snapshot's metadata is the metadata being
 * sent, which has none of them: Kotlin/JS reads such an `undefined` field as null, but a non-null `String` external
 * would wrap `undefined` here, so these are read untyped. (`size` reads as `NaN`, so 0, either way.)
 */
private fun FullMetadata.serverField(key: String): String? = jsGet(this, key).toKotlin() as? String

private fun String?.toEpochMillis(): Long = this?.let { parseDateStringToMillis(it).toLong() }?.takeIf { it >= 0 } ?: 0
