/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.auth

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import dev.gitlive.firebase.Unsubscribe
import dev.gitlive.firebase.auth.externals.ActionCodeURL
import dev.gitlive.firebase.auth.externals.ApplicationVerifier
import dev.gitlive.firebase.auth.externals.Auth
import dev.gitlive.firebase.auth.externals.AuthError
import dev.gitlive.firebase.auth.externals.User
import dev.gitlive.firebase.auth.externals.UserCredential
import dev.gitlive.firebase.auth.externals.applyActionCode
import dev.gitlive.firebase.auth.externals.checkActionCode
import dev.gitlive.firebase.auth.externals.confirmPasswordReset
import dev.gitlive.firebase.auth.externals.connectAuthEmulator
import dev.gitlive.firebase.auth.externals.createUserWithEmailAndPassword
import dev.gitlive.firebase.auth.externals.deleteUser
import dev.gitlive.firebase.auth.externals.fetchSignInMethodsForEmail
import dev.gitlive.firebase.auth.externals.getAdditionalUserInfo
import dev.gitlive.firebase.auth.externals.getAuth
import dev.gitlive.firebase.auth.externals.getIdTokenResult
import dev.gitlive.firebase.auth.externals.getMultiFactorResolver
import dev.gitlive.firebase.auth.externals.initializeRecaptchaConfig
import dev.gitlive.firebase.auth.externals.isSignInWithEmailLink
import dev.gitlive.firebase.auth.externals.linkWithCredential
import dev.gitlive.firebase.auth.externals.multiFactor
import dev.gitlive.firebase.auth.externals.onAuthStateChanged
import dev.gitlive.firebase.auth.externals.onIdTokenChanged
import dev.gitlive.firebase.auth.externals.reauthenticateWithCredential
import dev.gitlive.firebase.auth.externals.reload
import dev.gitlive.firebase.auth.externals.revokeAccessToken
import dev.gitlive.firebase.auth.externals.sendEmailVerification
import dev.gitlive.firebase.auth.externals.sendPasswordResetEmail
import dev.gitlive.firebase.auth.externals.sendSignInLinkToEmail
import dev.gitlive.firebase.auth.externals.signInAnonymously
import dev.gitlive.firebase.auth.externals.signInWithCredential
import dev.gitlive.firebase.auth.externals.signInWithCustomToken
import dev.gitlive.firebase.auth.externals.signInWithEmailAndPassword
import dev.gitlive.firebase.auth.externals.signInWithEmailLink
import dev.gitlive.firebase.auth.externals.signOut
import dev.gitlive.firebase.auth.externals.unlink
import dev.gitlive.firebase.auth.externals.updateCurrentUser
import dev.gitlive.firebase.auth.externals.updateEmail
import dev.gitlive.firebase.auth.externals.updatePassword
import dev.gitlive.firebase.auth.externals.updatePhoneNumber
import dev.gitlive.firebase.auth.externals.updateProfile
import dev.gitlive.firebase.auth.externals.useDeviceLanguage
import dev.gitlive.firebase.auth.externals.verifyBeforeUpdateEmail
import dev.gitlive.firebase.auth.externals.verifyPasswordResetCode
import dev.gitlive.firebase.externals.asJsAny
import dev.gitlive.firebase.externals.asJsArray
import dev.gitlive.firebase.externals.jsGet
import dev.gitlive.firebase.externals.jsIsArray
import dev.gitlive.firebase.externals.jsObject
import dev.gitlive.firebase.externals.jsObjectEntries
import dev.gitlive.firebase.externals.jsSet
import dev.gitlive.firebase.externals.jsTypeOf
import dev.gitlive.firebase.externals.json
import dev.gitlive.firebase.externals.parseDateStringToMillis
import dev.gitlive.firebase.externals.toJs
import dev.gitlive.firebase.externals.toKotlin
import dev.gitlive.firebase.externals.toKotlinString
import dev.gitlive.firebase.externals.toList
import kotlin.js.JsException
import kotlin.js.Promise
import dev.gitlive.firebase.auth.externals.AuthCredential as JsAuthCredential
import dev.gitlive.firebase.auth.externals.AuthResult as JsAuthResult
import dev.gitlive.firebase.auth.externals.EmailAuthProvider as JsEmailAuthProvider
import dev.gitlive.firebase.auth.externals.FacebookAuthProvider as JsFacebookAuthProvider
import dev.gitlive.firebase.auth.externals.GithubAuthProvider as JsGithubAuthProvider
import dev.gitlive.firebase.auth.externals.GoogleAuthProvider as JsGoogleAuthProvider
import dev.gitlive.firebase.auth.externals.MultiFactorAssertion as JsMultiFactorAssertion
import dev.gitlive.firebase.auth.externals.MultiFactorInfo as JsMultiFactorInfo
import dev.gitlive.firebase.auth.externals.MultiFactorResolver as JsMultiFactorResolver
import dev.gitlive.firebase.auth.externals.MultiFactorSession as JsMultiFactorSession
import dev.gitlive.firebase.auth.externals.MultiFactorUser as JsMultiFactorUser
import dev.gitlive.firebase.auth.externals.OAuthCredential as JsOAuthCredential
import dev.gitlive.firebase.auth.externals.OAuthProvider as JsOAuthProvider
import dev.gitlive.firebase.auth.externals.PhoneAuthProvider as JsPhoneAuthProvider
import dev.gitlive.firebase.auth.externals.PhoneMultiFactorGenerator as JsPhoneMultiFactorGenerator
import dev.gitlive.firebase.auth.externals.PhoneMultiFactorInfo as JsPhoneMultiFactorInfo
import dev.gitlive.firebase.auth.externals.TotpMultiFactorGenerator as JsTotpMultiFactorGenerator
import dev.gitlive.firebase.auth.externals.TotpSecret as JsTotpSecret
import dev.gitlive.firebase.auth.externals.TwitterAuthProvider as JsTwitterAuthProvider
import dev.gitlive.firebase.auth.externals.UserInfo as JsUserInfo

/** @property js The underlying Firebase JS SDK object. */
public actual class FirebaseAuth internal constructor(public val js: Auth) {
    private val authStateListeners = mutableMapOf<AuthStateListener, Unsubscribe>()
    private val idTokenListeners = mutableMapOf<IdTokenListener, Unsubscribe>()

    public actual val app: FirebaseApp get() = FirebaseApp.getInstance(js.app.name)

    /** The user whose sign-out is in flight; the JS SDK clears its current user only once the sign-out promise settles. */
    private var signingOut: User? = null

    public actual val currentUser: FirebaseUser?
        get() = rethrow { js.currentUser?.takeUnless { it == signingOut }?.let { FirebaseUserImpl(it, this) } }

    public actual val languageCode: String? get() = js.languageCode

    public actual val tenantId: String? get() = js.tenantId

    /** The auth domain of the app's options; the JS SDK only takes it when the app is initialized. */
    public actual val customAuthDomain: String? get() = js.config.authDomain

    public actual fun setLanguageCode(languageCode: String) {
        js.languageCode = languageCode
    }

    public actual fun setTenantId(tenantId: String) {
        js.tenantId = tenantId
    }

    public actual fun setCustomAuthDomain(customAuthDomain: String): Unit = throw UnsupportedOperationException("The JS SDK's auth domain is set in the app's options")

    public actual val firebaseAuthSettings: FirebaseAuthSettings = object : FirebaseAuthSettings() {
        override fun setAppVerificationDisabledForTesting(disabled: Boolean) {
            js.settings.appVerificationDisabledForTesting = disabled
        }
    }

    public actual fun useAppLanguage(): Unit = rethrow { useDeviceLanguage(js) }

    public actual fun useEmulator(host: String, port: Int): Unit = rethrow { connectAuthEmulator(js, "http://$host:$port") }

    /** Starts signing out; [currentUser] reads as null immediately, as on Android, while the JS SDK completes it asynchronously. */
    public actual fun signOut() {
        val user = js.currentUser ?: return
        signingOut = user
        rethrow { signOut(js) }.then(
            onFulfilled = {
                signingOut = null
                null
            },
            onRejected = {
                signingOut = null
                null
            },
        )
    }

    public actual fun isSignInWithEmailLink(link: String): Boolean = rethrow { isSignInWithEmailLink(js, link) }

    public actual fun addAuthStateListener(listener: AuthStateListener) {
        authStateListeners.getOrPut(listener) { onAuthStateChanged(js) { listener.onAuthStateChanged(this) } }
    }

    public actual fun removeAuthStateListener(listener: AuthStateListener) {
        authStateListeners.remove(listener)?.invoke()
    }

    public actual fun addIdTokenListener(listener: IdTokenListener) {
        idTokenListeners.getOrPut(listener) { onIdTokenChanged(js) { listener.onIdTokenChanged(this) } }
    }

    public actual fun removeIdTokenListener(listener: IdTokenListener) {
        idTokenListeners.remove(listener)?.invoke()
    }

    public actual fun applyActionCode(code: String): Task<Nothing?> = write(this) { applyActionCode(js, code) }

    public actual fun checkActionCode(code: String): Task<ActionCodeResult> = task(this, { checkActionCode(js, code) }) { info ->
        val operation = info.operation.toOperation()
        val email = info.data.email
        val details = when {
            email == null -> null
            operation == ActionCodeResult.RECOVER_EMAIL || operation == ActionCodeResult.VERIFY_BEFORE_CHANGE_EMAIL -> ActionCodeEmailInfoImpl(email, info.data.previousEmail.orEmpty())
            operation == ActionCodeResult.REVERT_SECOND_FACTOR_ADDITION && info.data.multiFactorInfo != null -> ActionCodeMultiFactorInfoImpl(email, info.data.multiFactorInfo!!.toCompat())
            else -> ActionCodeInfoImpl(email)
        }
        ActionCodeResultImpl(operation, details)
    }

    public actual fun confirmPasswordReset(code: String, newPassword: String): Task<Nothing?> = write(this) { confirmPasswordReset(js, code, newPassword) }

    public actual fun createUserWithEmailAndPassword(email: String, password: String): Task<AuthResult> = task(this, { createUserWithEmailAndPassword(js, email, password) }) { toAuthResult(it, this) }

    @Deprecated("Migrating off of this method is recommended as a security best-practice. Learn more in the Identity Platform documentation for [Email Enumeration Protection](https://cloud.google.com/identity-platform/docs/admin/email-enumeration-protection).")
    public actual fun fetchSignInMethodsForEmail(email: String): Task<SignInMethodQueryResult> = task(this, { fetchSignInMethodsForEmail(js, email) }) { methods -> SignInMethodQueryResultImpl(methods.toList().map { it.toKotlinString() }) }

    public actual fun initializeRecaptchaConfig(): Task<Nothing?> = write(this) { initializeRecaptchaConfig(js) }

    public actual fun revokeAccessToken(token: String): Task<Nothing?> = write(this) { revokeAccessToken(js, token) }

    public actual fun sendPasswordResetEmail(email: String): Task<Nothing?> = sendPasswordResetEmail(email, null)

    public actual fun sendPasswordResetEmail(email: String, actionCodeSettings: ActionCodeSettings?): Task<Nothing?> = write(this) { sendPasswordResetEmail(js, email, actionCodeSettings?.toJson()) }

    public actual fun sendSignInLinkToEmail(email: String, actionCodeSettings: ActionCodeSettings): Task<Nothing?> = write(this) { sendSignInLinkToEmail(js, email, actionCodeSettings.toJson()) }

    public actual fun signInAnonymously(): Task<AuthResult> = task(this, { signInAnonymously(js) }) { toAuthResult(it, this) }

    public actual fun signInWithCredential(credential: AuthCredential): Task<AuthResult> = task(this, { signInWithCredential(js, credential.js) }) { toAuthResult(it, this) }

    public actual fun signInWithCustomToken(token: String): Task<AuthResult> = task(this, { signInWithCustomToken(js, token) }) { toAuthResult(it, this) }

    public actual fun signInWithEmailAndPassword(email: String, password: String): Task<AuthResult> = task(this, { signInWithEmailAndPassword(js, email, password) }) { toAuthResult(it, this) }

    public actual fun signInWithEmailLink(email: String, link: String): Task<AuthResult> = task(this, { signInWithEmailLink(js, email, link) }) { toAuthResult(it, this) }

    public actual fun updateCurrentUser(user: FirebaseUser): Task<Nothing?> = write(this) { updateCurrentUser(js, user.js) }

    public actual fun verifyPasswordResetCode(code: String): Task<String> = task(this, { verifyPasswordResetCode(js, code) }) { it.toKotlinString() }

    public actual fun interface AuthStateListener {
        public actual fun onAuthStateChanged(auth: FirebaseAuth)
    }

    public actual fun interface IdTokenListener {
        public actual fun onIdTokenChanged(auth: FirebaseAuth)
    }

    override fun equals(other: Any?): Boolean = other is FirebaseAuth && other.js == js

    override fun hashCode(): Int = js.hashCode()

    override fun toString(): String = "FirebaseAuth(app=${js.app.name})"

    public actual companion object {
        private val instances = mutableMapOf<Auth, FirebaseAuth>()

        public actual fun getInstance(): FirebaseAuth = getInstance(FirebaseApp.getInstance())

        public actual fun getInstance(app: FirebaseApp): FirebaseAuth = rethrow { wrap(getAuth(app.js)) }

        /** The instance of the JS SDK's [auth]. */
        public fun wrap(auth: Auth): FirebaseAuth = instances.getOrPut(auth) { FirebaseAuth(auth) }
    }
}

/** @property js The underlying Firebase JS SDK object. */
public actual abstract class FirebaseUser actual constructor() : UserInfo {
    public abstract val js: User
    internal abstract val auth: FirebaseAuth
    public actual abstract val tenantId: String?
    public actual abstract val isAnonymous: Boolean
    public actual abstract val metadata: FirebaseUserMetadata?
    public actual abstract val multiFactor: MultiFactor
    public actual abstract val providerData: List<UserInfo>

    public actual fun delete(): Task<Nothing?> = write(auth) { deleteUser(js) }

    public actual fun getIdToken(forceRefresh: Boolean): Task<GetTokenResult> = task(auth, { getIdTokenResult(js, forceRefresh) }) { result -> GetTokenResult(result.token, result.claims.toKotlinMap()) }

    public actual fun linkWithCredential(credential: AuthCredential): Task<AuthResult> = task(auth, { linkWithCredential(js, credential.js) }) { toAuthResult(it, auth) }

    public actual fun reauthenticate(credential: AuthCredential): Task<Nothing?> = write(auth) { reauthenticateWithCredential(js, credential.js) }

    public actual fun reauthenticateAndRetrieveData(credential: AuthCredential): Task<AuthResult> = task(auth, { reauthenticateWithCredential(js, credential.js) }) { toAuthResult(it, auth) }

    public actual fun reload(): Task<Nothing?> = write(auth) { reload(js) }

    public actual fun sendEmailVerification(): Task<Nothing?> = sendEmailVerification(null)

    public actual fun sendEmailVerification(actionCodeSettings: ActionCodeSettings?): Task<Nothing?> = write(auth) { sendEmailVerification(js, actionCodeSettings?.toJson()) }

    public actual fun unlink(provider: String): Task<AuthResult> = task(auth, { unlink(js, provider) }) { AuthResultImpl(FirebaseUserImpl(it, auth), null, null) }

    @Deprecated("Use verifyBeforeUpdateEmail instead", ReplaceWith("verifyBeforeUpdateEmail(email)"))
    public actual fun updateEmail(email: String): Task<Nothing?> = write(auth) { updateEmail(js, email) }

    public actual fun updatePassword(password: String): Task<Nothing?> = write(auth) { updatePassword(js, password) }

    public actual fun updatePhoneNumber(credential: PhoneAuthCredential): Task<Nothing?> = write(auth) { updatePhoneNumber(js, credential.js) }

    public actual fun updateProfile(request: UserProfileChangeRequest): Task<Nothing?> = write(auth) {
        // A field set to null is sent as null, not left out: the JS SDK clears the attributes it receives as null.
        val profile = jsObject()
        if (request.displayNameSet) jsSet(profile, "displayName", request.displayName.toJs())
        if (request.photoUriSet) jsSet(profile, "photoURL", request.photoUri.toJs())
        updateProfile(js, profile)
    }

    public actual fun verifyBeforeUpdateEmail(newEmail: String): Task<Nothing?> = verifyBeforeUpdateEmail(newEmail, null)

    public actual fun verifyBeforeUpdateEmail(newEmail: String, actionCodeSettings: ActionCodeSettings?): Task<Nothing?> = write(auth) { verifyBeforeUpdateEmail(js, newEmail, actionCodeSettings?.toJson()) }

    override fun equals(other: Any?): Boolean = other is FirebaseUser && other.js == js

    override fun hashCode(): Int = js.hashCode()

    override fun toString(): String = "FirebaseUser(uid=$uid)"
}

internal class FirebaseUserImpl(override val js: User, override val auth: FirebaseAuth) : FirebaseUser() {
    override val uid: String get() = js.uid
    override val providerId: String get() = js.providerId
    override val displayName: String? get() = js.displayName
    override val email: String? get() = js.email
    override val phoneNumber: String? get() = js.phoneNumber
    override val isEmailVerified: Boolean get() = js.emailVerified
    override val photoUrl: String? get() = js.photoURL
    override val tenantId: String? get() = js.tenantId
    override val isAnonymous: Boolean get() = js.isAnonymous
    override val metadata: FirebaseUserMetadata? get() = FirebaseUserMetadataImpl(js.metadata.creationTime.toTimestamp(), js.metadata.lastSignInTime.toTimestamp())
    override val multiFactor: MultiFactor get() = MultiFactorImpl(multiFactor(js), auth)
    override val providerData: List<UserInfo> get() = js.providerData.toList().map { it.toCompat() }
}

private fun JsUserInfo.toCompat(): UserInfo = UserInfoImpl(uid, providerId, displayName, email, phoneNumber, false, photoURL)

/** Milliseconds since the epoch of a JS SDK date string (as `new Date(...)` parses it), or 0 when absent. */
private fun String?.toTimestamp(): Long = this?.let { parseDateStringToMillis(it).toLong() } ?: 0L

internal class MultiFactorImpl(public val js: JsMultiFactorUser, private val auth: FirebaseAuth) : MultiFactor() {
    override val enrolledFactors: List<MultiFactorInfo> get() = js.enrolledFactors.toList().map { it.toCompat() }

    override fun enroll(assertion: MultiFactorAssertion, displayName: String?): Task<Nothing?> = write(auth) { js.enroll(assertion.native.toJsAssertion(), displayName) }

    override fun getSession(): Task<MultiFactorSession> = task(auth, { js.getSession() }) { MultiFactorSessionImpl(it) }

    override fun unenroll(info: MultiFactorInfo): Task<Nothing?> = write(auth) { info.native?.let { js.unenroll(it.toJsMultiFactorInfo()) } ?: js.unenroll(info.uid) }

    override fun unenroll(factorUid: String): Task<Nothing?> = write(auth) { js.unenroll(factorUid) }
}

internal class MultiFactorResolverImpl(public val js: JsMultiFactorResolver, override val firebaseAuth: FirebaseAuth) : MultiFactorResolver() {
    override val hints: List<MultiFactorInfo> get() = js.hints.toList().map { it.toCompat() }
    override val session: MultiFactorSession get() = MultiFactorSessionImpl(js.session)

    override fun resolveSignIn(assertion: MultiFactorAssertion): Task<AuthResult> = task(firebaseAuth, { js.resolveSignIn(assertion.native.toJsAssertion()) }) { toAuthResult(it, firebaseAuth) }
}

internal class TotpSecretImpl(public val js: JsTotpSecret) : TotpSecret {
    override val sharedSecretKey: String get() = js.secretKey

    override fun generateQrCodeUrl(accountName: String, issuer: String): String = js.generateQrCodeUrl(accountName, issuer)

    override fun openInOtpApp(qrCodeUrl: String) {
        openInWindow(qrCodeUrl)
    }
}

/** Opens [url] with `window.open`, when there is a window: the wasmJs target also runs in Node.js. */
private fun openInWindow(url: String) {
    js("if (typeof window !== 'undefined') window.open(url);")
}

internal fun JsMultiFactorInfo.toCompat(): MultiFactorInfo {
    val enrollmentTimestamp = enrollmentTime.toTimestamp()
    return when (factorId) {
        "phone" -> PhoneMultiFactorInfo(uid, displayName, enrollmentTimestamp, toJsPhoneMultiFactorInfo().phoneNumber, this)
        else -> TotpMultiFactorInfo(uid, displayName, enrollmentTimestamp, this)
    }
}

/** The JS SDK's credential behind a credential of the layer. */
public val AuthCredential.js: JsAuthCredential get() = platform.toJsCredential()

internal fun JsAuthCredential.toCompat(): AuthCredential = when (providerId) {
    EmailAuthProvider.PROVIDER_ID -> EmailAuthCredential(this, signInMethod)
    FacebookAuthProvider.PROVIDER_ID -> FacebookAuthCredential(this)
    GithubAuthProvider.PROVIDER_ID -> GithubAuthCredential(this)
    GoogleAuthProvider.PROVIDER_ID -> GoogleAuthCredential(this)
    TwitterAuthProvider.PROVIDER_ID -> TwitterAuthCredential(this)
    PhoneAuthProvider.PROVIDER_ID -> PhoneAuthCredential(this, null)
    else -> NativeCredential(this, providerId, signInMethod)
}

private fun JsOAuthCredential.toCompat(): OAuthCredential = OAuthCredentialImpl(this, providerId, idToken, accessToken, secret)

internal fun toAuthResult(result: JsAuthResult, auth: FirebaseAuth): AuthResult = AuthResultImpl(
    result.user?.let { FirebaseUserImpl(it, auth) },
    runCatching { JsOAuthProvider.credentialFromResult(result.toUserCredential()) }.getOrNull()?.toCompat(),
    getAdditionalUserInfo(result.toUserCredential())?.let { AdditionalUserInfoImpl(it.providerId, it.username, it.profile?.toKotlinMap(), it.isNewUser) },
)

/** The settings as the JS SDK's `ActionCodeSettings`; the fields that are not set are left out (wasm has no `undefined`). */
internal fun ActionCodeSettings.toJson(): JsAny {
    val settings = json("url" to url)
    androidPackageName?.let { jsSet(settings, "android", json("installApp" to androidInstallApp, "minimumVersion" to androidMinimumVersion, "packageName" to it)) }
    linkDomain?.let { jsSet(settings, "linkDomain", it.toJs()) }
    dynamicLinkDomain?.let { jsSet(settings, "dynamicLinkDomain", it.toJs()) }
    jsSet(settings, "handleCodeInApp", canHandleCodeInApp().toJs())
    iosBundle?.let { jsSet(settings, "iOS", json("bundleId" to it)) }
    return settings
}

private fun String.toOperation(): Int = when (this) {
    "PASSWORD_RESET" -> ActionCodeResult.PASSWORD_RESET
    "VERIFY_EMAIL" -> ActionCodeResult.VERIFY_EMAIL
    "RECOVER_EMAIL" -> ActionCodeResult.RECOVER_EMAIL
    "EMAIL_SIGNIN" -> ActionCodeResult.SIGN_IN_WITH_EMAIL_LINK
    "VERIFY_AND_CHANGE_EMAIL" -> ActionCodeResult.VERIFY_BEFORE_CHANGE_EMAIL
    "REVERT_SECOND_FACTOR_ADDITION" -> ActionCodeResult.REVERT_SECOND_FACTOR_ADDITION
    else -> ActionCodeResult.ERROR
}

/** A JS value as Kotlin values: objects as maps, arrays as lists, primitives as their Kotlin equivalents. */
private fun JsAny?.toKotlinValue(): Any? = when {
    this == null -> null
    jsIsArray(this) -> asJsArray().toList().map { it.toKotlinValue() }
    jsTypeOf(this) == "object" -> toKotlinMap()
    else -> toKotlin()
}

/** The properties of a JS object as a map, their values converted with [toKotlinValue]. */
private fun JsAny.toKotlinMap(): Map<String, Any?> = jsObjectEntries(this).toList().associate { entry -> entry[0].toKotlin().toString() to entry[1].toKotlinValue() }

/*
 * The layer keeps the JS SDK's objects as `Any` (credentials, assertions, infos, sessions, verifiers) and the JS SDK
 * returns some objects typed as a related interface; Kotlin/Wasm has no `unsafeCast` for these, and casts to an
 * external interface are not checked at runtime.
 */

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
private fun Any.toJsCredential(): JsAuthCredential = this as JsAuthCredential

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
private fun Any.toJsAssertion(): JsMultiFactorAssertion = this as JsMultiFactorAssertion

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
private fun Any.toJsMultiFactorInfo(): JsMultiFactorInfo = this as JsMultiFactorInfo

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
private fun Any.toJsSession(): JsMultiFactorSession = this as JsMultiFactorSession

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
private fun Any.toJsVerifier(): ApplicationVerifier = this as ApplicationVerifier

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
private fun JsMultiFactorInfo.toJsPhoneMultiFactorInfo(): JsPhoneMultiFactorInfo = this as JsPhoneMultiFactorInfo

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
private fun JsAuthResult.toUserCredential(): UserCredential = this as UserCredential

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
private fun JsAny.toAuthError(): AuthError = this as AuthError

/**
 * The JS SDK's `AuthError` (with a `code` such as `auth/wrong-password`) as the Android SDK's exception. A synchronous
 * failure arrives as the [JsException] wrapping the thrown JS value.
 */
internal fun Throwable.toAuthException(auth: FirebaseAuth?): FirebaseException = when (this) {
    is FirebaseException -> this
    is JsException -> thrownValue?.let { errorToAuthException(it, this, auth) } ?: FirebaseAuthException("ERROR_UNKNOWN", message ?: toString(), this)
    else -> FirebaseAuthException("ERROR_UNKNOWN", message ?: toString(), this)
}

/**
 * A promise rejection carries the JS error itself rather than a [Throwable]: it becomes the [JsException] it would be
 * thrown as, or the Kotlin exception it wraps, when it was thrown from Kotlin.
 */
internal fun JsAny.toAuthException(auth: FirebaseAuth?): FirebaseException = toThrowableOrNull()?.toAuthException(auth)
    ?: errorToAuthException(this, Exception(jsGet(this, "message").toKotlin()?.toString()), auth)

private fun errorToAuthException(error: JsAny, cause: Throwable, auth: FirebaseAuth?): FirebaseException {
    val message = jsGet(error, "message").toKotlin() as? String ?: cause.message
    val code = (jsGet(error, "code").toKotlin() as? String)?.substringAfter("auth/", "")?.takeIf { it.isNotEmpty() } ?: return FirebaseAuthException("ERROR_UNKNOWN", message ?: cause.toString(), cause)
    val errorCode = "ERROR_" + code.uppercase().replace('-', '_')
    val resolver = if (code == "multi-factor-auth-required" && auth != null) runCatching { MultiFactorResolverImpl(getMultiFactorResolver(auth.js, error.toAuthError()), auth) }.getOrNull() else null
    return authException(
        errorCode,
        message ?: errorCode,
        AuthErrorDetails(
            cause = cause,
            resolver = resolver,
            email = jsGet(error, "customData")?.let { jsGet(it, "email") }.toKotlin() as? String,
            updatedCredential = runCatching { JsOAuthProvider.credentialFromError(error.toAuthError()) }.getOrNull()?.toCompat(),
        ),
    )
}

/**
 * A task completed by the JS SDK's promise, with its value converted by [result]. A failure of [result] fails the task,
 * as it rejects the chained promise in jsMain.
 */
internal inline fun <T : JsAny?, R> task(auth: FirebaseAuth?, start: () -> Promise<T>, crossinline result: (T) -> R): Task<R> {
    val source = TaskCompletionSource<R>()
    try {
        start().then(
            onFulfilled = { value ->
                runCatching { result(value) }.fold({ source.setResult(it) }, { source.setException(it.toAuthException(auth)) })
                null
            },
            onRejected = {
                source.setException(it.toAuthException(auth))
                null
            },
        )
    } catch (e: Throwable) {
        source.setException(e.toAuthException(auth))
    }
    return source.task
}

internal inline fun write(auth: FirebaseAuth?, start: () -> Promise<JsAny?>): Task<Nothing?> = task(auth, start) { null }

internal inline fun <R> rethrow(function: () -> R): R = try {
    function()
} catch (e: Throwable) {
    throw e.toAuthException(null)
}

internal actual fun nativeEmailCredential(email: String, password: String): Any = rethrow { JsEmailAuthProvider.credential(email, password) }

internal actual fun nativeEmailLinkCredential(email: String, emailLink: String): Any = rethrow { JsEmailAuthProvider.credentialWithLink(email, emailLink) }

internal actual fun nativeFacebookCredential(accessToken: String): Any = rethrow { JsFacebookAuthProvider.credential(accessToken) }

internal actual fun nativeGithubCredential(token: String): Any = rethrow { JsGithubAuthProvider.credential(token) }

internal actual fun nativeGoogleCredential(idToken: String?, accessToken: String?): Any = rethrow { JsGoogleAuthProvider.credential(idToken, accessToken) }

internal actual fun nativeTwitterCredential(token: String, secret: String): Any = rethrow { JsTwitterAuthProvider.credential(token, secret) }

/** The tokens that are not set are left out of the options, and the access token argument is omitted without one (wasm has no `undefined`). */
internal actual fun nativeOAuthCredential(providerId: String, idToken: String?, accessToken: String?, rawNonce: String?): Any = rethrow {
    val options = jsObject()
    accessToken?.let { jsSet(options, "accessToken", it.toJs()) }
    idToken?.let { jsSet(options, "idToken", it.toJs()) }
    rawNonce?.let { jsSet(options, "rawNonce", it.toJs()) }
    val provider = JsOAuthProvider(providerId)
    if (accessToken != null) provider.credential(options, accessToken) else provider.credential(options)
}

internal actual fun nativePhoneCredential(verificationId: String, smsCode: String): Any = rethrow { JsPhoneAuthProvider.credential(verificationId, smsCode) }

internal actual fun nativeVerifyPhoneNumber(options: PhoneAuthOptions) {
    val verifier = options.verificationContext?.toJsVerifier()
    val info: Any = when {
        options.multiFactorHint != null -> json("multiFactorHint" to options.multiFactorHint.native, "session" to (options.multiFactorSession as MultiFactorSessionImpl).native)
        options.multiFactorSession != null -> json("phoneNumber" to options.phoneNumber, "session" to (options.multiFactorSession as MultiFactorSessionImpl).native)
        else -> options.phoneNumber!!
    }
    val provider = JsPhoneAuthProvider(options.firebaseAuth.js)
    val verification = try {
        if (info is String) provider.verifyPhoneNumber(info, verifier) else provider.verifyPhoneNumber(info.asJsAny(), verifier)
    } catch (e: Throwable) {
        options.callbacks.onVerificationFailed(e.toAuthException(options.firebaseAuth))
        return
    }
    verification.then(
        onFulfilled = {
            options.codeSent(it.toKotlinString())
            null
        },
        onRejected = {
            options.callbacks.onVerificationFailed(it.toAuthException(options.firebaseAuth))
            null
        },
    )
}

internal actual fun nativePhoneMultiFactorAssertion(credential: PhoneAuthCredential): Any = rethrow { JsPhoneMultiFactorGenerator.assertion(credential.js) }

internal actual fun nativeTotpGenerateSecret(session: MultiFactorSession): Task<TotpSecret> = task(null, { JsTotpMultiFactorGenerator.generateSecret((session as MultiFactorSessionImpl).native.toJsSession()) }) { TotpSecretImpl(it) }

internal actual fun nativeTotpAssertionForEnrollment(secret: TotpSecret, oneTimePassword: String): Any = rethrow { JsTotpMultiFactorGenerator.assertionForEnrollment((secret as TotpSecretImpl).js, oneTimePassword) }

internal actual fun nativeTotpAssertionForSignIn(enrollmentId: String, oneTimePassword: String): Any = rethrow { JsTotpMultiFactorGenerator.assertionForSignIn(enrollmentId, oneTimePassword) }

internal actual fun parseActionCodeUrl(link: String): ActionCodeUrl? = rethrow { ActionCodeURL.parseLink(link)?.let { ActionCodeUrl(it.apiKey, it.code, it.continueUrl, it.languageCode, it.operation.toOperation()) } }

/** Sets the reCAPTCHA (or other `ApplicationVerifier`) of the verification; the JS SDK requires one in browsers. */
public fun PhoneAuthOptions.Builder.setApplicationVerifier(verifier: ApplicationVerifier): PhoneAuthOptions.Builder = apply { verificationContext = verifier }
