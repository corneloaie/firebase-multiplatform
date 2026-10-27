/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.auth.internal

import com.google.firebase.auth.FirebaseAuth
import io.github.corneloaie.firebase.auth.externals.signOut
import io.github.corneloaie.firebase.externals.awaitUnit

internal actual suspend fun FirebaseAuth.signOutAwaiting() {
    signOut(js).awaitUnit()
}
