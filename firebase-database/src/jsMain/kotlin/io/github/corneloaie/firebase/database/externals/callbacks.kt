/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.database.externals

public typealias ChangeSnapshotCallback = (data: DataSnapshot, previousChildName: String?) -> Unit
public typealias ValueSnapshotCallback = (data: DataSnapshot) -> Unit
public typealias CancelCallback = (error: Throwable) -> Unit
