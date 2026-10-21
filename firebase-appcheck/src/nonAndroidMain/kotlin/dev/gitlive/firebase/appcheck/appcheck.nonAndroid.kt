/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.appcheck

import com.google.firebase.appcheck.FirebaseAppCheck as CompatFirebaseAppCheck

/** The iOS SDK's notification center and the JS SDK both allow a listener to be removed while a token is delivered. */
internal actual fun CompatFirebaseAppCheck.removeListenerAfterDelivery(listener: CompatFirebaseAppCheck.AppCheckListener) {
    removeAppCheckListener(listener)
}
