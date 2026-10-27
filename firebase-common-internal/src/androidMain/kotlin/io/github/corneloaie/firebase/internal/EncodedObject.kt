@file:JvmName("AndroidEncodedObject")

package io.github.corneloaie.firebase.internal

public val EncodedObject.android: Map<String, Any?> get() = getRaw()

@PublishedApi
internal actual fun Any.asNativeMap(): Map<*, *>? = this as? Map<*, *>
