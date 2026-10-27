@file:JvmName("AppCheckKt")
@file:JvmMultifileClass

package io.github.corneloaie.firebase.appcheck

import android.os.Handler
import android.os.Looper
import com.google.firebase.appcheck.FirebaseAppCheck as AndroidFirebaseAppCheck

/** The underlying Firebase Android SDK object. */
public val FirebaseAppCheck.android: AndroidFirebaseAppCheck get() = compat

/**
 * The Android SDK keeps its listeners in an `ArrayList` that it iterates on the main thread to deliver a token, so a
 * listener removed from inside that delivery would end it with a `ConcurrentModificationException`: the removal is
 * queued on the main thread, after the delivery.
 */
internal actual fun AndroidFirebaseAppCheck.removeListenerAfterDelivery(listener: AndroidFirebaseAppCheck.AppCheckListener) {
    Handler(Looper.getMainLooper()).post { removeAppCheckListener(listener) }
}
