/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck

import kotlin.jvm.JvmName
import kotlin.jvm.JvmSynthetic

/*
 * Plain common declarations: they are identical on every platform, so no expect/actual is needed. On Android they are
 * still compiled, verified against firebase-appcheck and stripped like the header stubs (see buildSrc
 * utils/HeaderStubs.kt).
 */

/**
 * A token the App Check backend issues once it has attested the app, mirroring
 * `com.google.firebase.appcheck.AppCheckToken`.
 *
 * The Android SDK's class is Java, so Android code subclasses it by overriding [getToken] and [getExpireTimeMillis],
 * for the tokens of an [AppCheckProvider] of its own, and reads the getters as the properties [token] and
 * [expireTimeMillis]. Both compile here as well, with one difference: those two properties are declared here, so a
 * subclass cannot declare properties of its own with these names (`private val token` in the constructor of a
 * subclass needs another name, such as `value`).
 */
public abstract class AppCheckToken {
    /** Returns the raw JWT attesting the app's identity. */
    public abstract fun getToken(): String

    /** Returns when the token expires, in milliseconds since the epoch. */
    public abstract fun getExpireTimeMillis(): Long

    // On Android, Kotlin reads the Java getters as these properties, so they are synthetic in the stripped header stub:
    // code shipped for Android calls the getters instead.

    /** The raw JWT attesting the app's identity: [getToken]. */
    @get:JvmSynthetic
    @get:JvmName("tokenProperty")
    public val token: String get() = getToken()

    /** When the token expires, in milliseconds since the epoch: [getExpireTimeMillis]. */
    @get:JvmSynthetic
    @get:JvmName("expireTimeMillisProperty")
    public val expireTimeMillis: Long get() = getExpireTimeMillis()
}
