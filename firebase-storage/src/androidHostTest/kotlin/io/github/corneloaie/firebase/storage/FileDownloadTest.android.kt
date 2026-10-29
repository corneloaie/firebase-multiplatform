/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.storage

actual fun temporaryFile(name: String): Any = java.io.File.createTempFile(name, null)

actual val supportsFileDownloads: Boolean = true

actual val uploadProgressIncludesOverhead: Boolean = false
