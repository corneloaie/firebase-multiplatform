@file:JvmName("AppCheckKt")
@file:JvmMultifileClass

package dev.gitlive.firebase.appcheck

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseApp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import com.google.firebase.appcheck.AppCheckProviderFactory as CompatAppCheckProviderFactory
import com.google.firebase.appcheck.AppCheckToken as CompatAppCheckToken
import com.google.firebase.appcheck.FirebaseAppCheck as CompatFirebaseAppCheck
import com.google.firebase.appcheck.appCheck as compatAppCheck

// The Android-SDK-shaped entry points are reached through the `Firebase.appCheck` extensions (FirebaseAppCheckKt),
// which are real static methods on every platform, rather than the companion object of the header stub.

/** Returns the [FirebaseAppCheck] of the default [FirebaseApp]. */
public val Firebase.appCheck: FirebaseAppCheck
    get() = FirebaseAppCheck(com.google.firebase.Firebase.compatAppCheck)

/** Returns the [FirebaseAppCheck] of [app]. */
public fun Firebase.appCheck(app: FirebaseApp): FirebaseAppCheck = FirebaseAppCheck(com.google.firebase.Firebase.compatAppCheck(app.compat))

/**
 * Firebase App Check: attests that requests to Firebase backends come from your app, and gives you tokens for your
 * own backend.
 *
 * Install a provider factory first: one of the factories of the `com.google.firebase.appcheck` packages
 * ([com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory],
 * [com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory],
 * [com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory]), a platform-specific one of this package,
 * or your own [com.google.firebase.appcheck.AppCheckProviderFactory]. When a factory takes effect differs between
 * platforms; see [com.google.firebase.appcheck.FirebaseAppCheck].
 *
 * @property compat The Android-SDK-shaped [com.google.firebase.appcheck.FirebaseAppCheck] this wraps.
 */
public class FirebaseAppCheck internal constructor(public val compat: CompatFirebaseAppCheck) {
    /**
     * Installs [factory], replacing the factory installed before.
     *
     * @param isTokenAutoRefreshEnabled whether App Check refreshes its token automatically; `null` follows the app's
     *   data collection setting (the iOS SDK keeps its own default there).
     */
    public fun installAppCheckProviderFactory(factory: CompatAppCheckProviderFactory, isTokenAutoRefreshEnabled: Boolean? = null) {
        if (isTokenAutoRefreshEnabled == null) {
            compat.installAppCheckProviderFactory(factory)
        } else {
            compat.installAppCheckProviderFactory(factory, isTokenAutoRefreshEnabled)
        }
    }

    /** Sets whether App Check refreshes its token automatically before it expires. */
    public fun setTokenAutoRefreshEnabled(isTokenAutoRefreshEnabled: Boolean) {
        compat.setTokenAutoRefreshEnabled(isTokenAutoRefreshEnabled)
    }

    /**
     * Returns an App Check token. Use it only for requests to your own backend: requests to Firebase backends get a
     * token automatically.
     *
     * @param forceRefresh whether to request a new token even when the cached one is still valid. Pass `true` only
     *   when your backend rejected the cached token.
     */
    public suspend fun getAppCheckToken(forceRefresh: Boolean = false): AppCheckToken = AppCheckToken(compat.getAppCheckToken(forceRefresh).await())

    /**
     * Returns a limited-use App Check token, for a backend with replay protection. It does not replace the token that
     * [getAppCheckToken] caches.
     */
    public suspend fun getLimitedUseAppCheckToken(): AppCheckToken = AppCheckToken(compat.getLimitedUseAppCheckToken().await())

    /**
     * Each new App Check token, from when the flow is collected. On Android and JS the flow starts with the cached
     * token when it is still valid; the iOS SDK reports new tokens only. A token's expiry emits nothing.
     */
    public val appCheckTokenChanged: Flow<AppCheckToken>
        get() = callbackFlow {
            val listener = CompatFirebaseAppCheck.AppCheckListener { trySend(AppCheckToken(it)) }
            compat.addAppCheckListener(listener)
            awaitClose { compat.removeListenerAfterDelivery(listener) }
        }

    override fun equals(other: Any?): Boolean = other is FirebaseAppCheck && other.compat == compat

    override fun hashCode(): Int = compat.hashCode()

    override fun toString(): String = "FirebaseAppCheck($compat)"
}

/**
 * A token the App Check backend issued once it had attested the app.
 *
 * @property compat The Android-SDK-shaped [com.google.firebase.appcheck.AppCheckToken] this wraps.
 */
public class AppCheckToken internal constructor(public val compat: CompatAppCheckToken) {
    /** The raw JWT to send to your backend, which verifies it with the Firebase Admin SDK. */
    public val token: String get() = compat.getToken()

    /**
     * When the token expires, in milliseconds since the epoch. The JS SDK does not report it: there it is read from
     * the token's `exp` claim, and is `0` for a token that is not a JWT.
     */
    public val expireTimeMillis: Long get() = compat.getExpireTimeMillis()

    /** Destructures the token: its [token]. */
    public operator fun component1(): String = token

    /** Destructures the token: its [expireTimeMillis]. */
    public operator fun component2(): Long = expireTimeMillis

    override fun equals(other: Any?): Boolean = other is AppCheckToken && other.token == token && other.expireTimeMillis == expireTimeMillis

    override fun hashCode(): Int = 31 * token.hashCode() + expireTimeMillis.hashCode()

    /** Leaves the token itself out: it is a credential. */
    override fun toString(): String = "AppCheckToken(expireTimeMillis=$expireTimeMillis)"
}

/**
 * Unregisters [listener] once the token being delivered, if any, has reached every listener. The Android SDK delivers
 * a token by iterating its listener list on the main thread, so a collector that stops at a token (`first()` on
 * `Dispatchers.Main.immediate`) must not remove its listener from inside that delivery.
 */
internal expect fun CompatFirebaseAppCheck.removeListenerAfterDelivery(listener: CompatFirebaseAppCheck.AppCheckListener)
