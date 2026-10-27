/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.storage

import com.google.firebase.storage.toUint8Array
import org.khronos.webgl.Int8Array
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.toByteArray
import io.github.corneloaie.firebase.storage.externals.FirebaseStorage as JsFirebaseStorage
import io.github.corneloaie.firebase.storage.externals.StorageReference as JsStorageReference

/** The underlying Firebase JS SDK object. */
public val FirebaseStorage.js: JsFirebaseStorage get() = compat.js

/** The underlying Firebase JS SDK object. */
public val StorageReference.js: JsStorageReference get() = compat.js

public actual typealias File = org.w3c.files.File

public actual class Data(public val data: Uint8Array)

public actual fun Data.toByteArray(): ByteArray = Int8Array(data.buffer, data.byteOffset, data.length).toByteArray()

// A Kotlin/Wasm ByteArray lives in Wasm memory, not in a JS buffer: toUint8Array copies it, so the Data does not share it
public actual fun ByteArray.toData(): Data = Data(toUint8Array())

internal actual val File.platformFile: Any get() = this
