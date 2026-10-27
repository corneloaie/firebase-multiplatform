/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.auth

import swiftPMImport.io.github.corneloaie.firebase.auth.FIRAuth
import swiftPMImport.io.github.corneloaie.firebase.auth.FIRAuthUIDelegateProtocol
import swiftPMImport.io.github.corneloaie.firebase.auth.FIRMultiFactor
import swiftPMImport.io.github.corneloaie.firebase.auth.FIRMultiFactorAssertion
import swiftPMImport.io.github.corneloaie.firebase.auth.FIRMultiFactorInfo
import swiftPMImport.io.github.corneloaie.firebase.auth.FIRMultiFactorResolver
import swiftPMImport.io.github.corneloaie.firebase.auth.FIRMultiFactorSession
import swiftPMImport.io.github.corneloaie.firebase.auth.FIRPhoneAuthCredential
import swiftPMImport.io.github.corneloaie.firebase.auth.FIRPhoneAuthProvider
import swiftPMImport.io.github.corneloaie.firebase.auth.FIRPhoneMultiFactorGenerator
import swiftPMImport.io.github.corneloaie.firebase.auth.FIRPhoneMultiFactorInfo
import swiftPMImport.io.github.corneloaie.firebase.auth.FIRTOTPMultiFactorGenerator
import swiftPMImport.io.github.corneloaie.firebase.auth.FIRTOTPSecret
import swiftPMImport.io.github.corneloaie.firebase.auth.FIRUser
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import io.github.corneloaie.firebase.UnsupportedOnApple
import platform.Foundation.NSError

/*
 * Phone number verification and multi-factor authentication: the Firebase iOS SDK only provides them on iOS.
 */

internal actual fun FIRAuth.compatSettings(): FirebaseAuthSettings = object : FirebaseAuthSettings() {
    override fun setAppVerificationDisabledForTesting(disabled: Boolean) {
        settings()?.setAppVerificationDisabledForTesting(disabled)
    }
}

internal actual fun FIRAuth.initializeRecaptchaConfigCompat(completion: (NSError?) -> Unit) {
    initializeRecaptchaConfigWithCompletion(completion)
}

internal actual fun FIRUser.updatePhoneNumberCompat(credential: PhoneAuthCredential, completion: (NSError?) -> Unit) {
    updatePhoneNumberCredential(credential.ios as FIRPhoneAuthCredential, completion)
}

internal actual fun FIRUser.compatMultiFactor(auth: FirebaseAuth): MultiFactor = MultiFactorImpl(multiFactor(), auth)

internal actual fun NSError.multiFactorResolver(auth: FirebaseAuth): MultiFactorResolver? = (userInfo["FIRAuthErrorUserInfoMultiFactorResolverKey"] as? FIRMultiFactorResolver)?.let { MultiFactorResolverImpl(it, auth) }

internal class MultiFactorImpl(val ios: FIRMultiFactor, private val auth: FirebaseAuth) : MultiFactor() {
    override val enrolledFactors: List<MultiFactorInfo> get() = ios.enrolledFactors().mapNotNull { (it as? FIRMultiFactorInfo)?.toCompat() }

    override fun enroll(assertion: MultiFactorAssertion, displayName: String?): Task<Nothing?> = write(auth) { ios.enrollWithAssertion(assertion.native as FIRMultiFactorAssertion, displayName, it) }

    override fun getSession(): Task<MultiFactorSession> = task(auth) { completion ->
        ios.getSessionWithCompletion { session, error -> completion(session?.let { MultiFactorSessionImpl(it, auth) }, error) }
    }

    override fun unenroll(info: MultiFactorInfo): Task<Nothing?> = write(auth) { completion ->
        (info.native as? FIRMultiFactorInfo)?.let { ios.unenrollWithInfo(it, completion) } ?: ios.unenrollWithFactorUID(info.uid, completion)
    }

    override fun unenroll(factorUid: String): Task<Nothing?> = write(auth) { ios.unenrollWithFactorUID(factorUid, it) }
}

internal class MultiFactorResolverImpl(val ios: FIRMultiFactorResolver, override val firebaseAuth: FirebaseAuth) : MultiFactorResolver() {
    override val hints: List<MultiFactorInfo> get() = ios.hints().mapNotNull { (it as? FIRMultiFactorInfo)?.toCompat() }
    override val session: MultiFactorSession get() = MultiFactorSessionImpl(ios.session(), firebaseAuth)

    override fun resolveSignIn(assertion: MultiFactorAssertion): Task<AuthResult> = task(firebaseAuth) { completion ->
        ios.resolveSignInWithAssertion(assertion.native as FIRMultiFactorAssertion) { result, error -> completion(result?.toCompat(firebaseAuth), error) }
    }
}

/** @property auth The instance of the user the secret was generated for, which [generateQrCodeUrl] takes its defaults from. */
internal class TotpSecretImpl(val ios: FIRTOTPSecret, private val auth: FirebaseAuth?) : TotpSecret {
    override val sharedSecretKey: String get() = ios.sharedSecretKey()

    override fun generateQrCodeUrl(accountName: String, issuer: String): String = ios.generateQRCodeURLWithAccountName(accountName, issuer)

    /**
     * As on Android: the signed-in user's email at the app's name, which is `[DEFAULT]` for the default app as on
     * Android and JS (the iOS SDK names it `__FIRAPP_DEFAULT`).
     */
    override fun generateQrCodeUrl(): String {
        val auth = auth ?: FirebaseAuth.getInstance()
        val user = checkNotNull(auth.currentUser) { "Current user cannot be null, since user is required to be logged in to enroll for TOTP MFA." }
        val email = user.email
        require(!email.isNullOrEmpty()) { "Email cannot be empty, since verified email is required to use MFA." }
        val appName = auth.app.name
        return generateQrCodeUrl(email, if (appName == FirebaseApp.DEFAULT_APP_NAME) ANDROID_DEFAULT_APP_NAME else appName)
    }

    override fun openInOtpApp(qrCodeUrl: String) {
        ios.openInOTPAppWithQRCodeURL(qrCodeUrl)
    }

    @UnsupportedOnApple
    override val codeIntervalSeconds: Int get() = unsupported("codeIntervalSeconds")

    override val codeLength: Int get() = qrCodeParameter("digits", "codeLength").toIntOrNull() ?: unsupported("codeLength")

    @UnsupportedOnApple
    override val enrollmentCompletionDeadline: Long get() = unsupported("enrollmentCompletionDeadline")

    override val hashAlgorithm: String get() = qrCodeParameter("algorithm", "hashAlgorithm")

    @UnsupportedOnApple
    override val sessionInfo: String get() = unsupported("sessionInfo")

    /**
     * The parameter [name] of the secret's QR code URL (`otpauth://totp/...?secret=...&algorithm=SHA1&digits=6`, in the
     * Key Uri Format), the only place the iOS SDK exposes the algorithm and the code length; it returns no URL without them.
     */
    private fun qrCodeParameter(name: String, member: String): String = ios.generateQRCodeURLWithAccountName("account", "issuer")
        .substringAfter('?', "")
        .split('&')
        .firstNotNullOfOrNull { parameter -> parameter.takeIf { it.startsWith("$name=") }?.substringAfter('=') }
        ?.takeIf { it.isNotEmpty() }
        ?: unsupported(member)

    private fun unsupported(member: String): Nothing = throw UnsupportedOperationException("TotpSecret.$member is not exposed by the Firebase iOS SDK")

    private companion object {
        /** The Android and JS SDKs' name of the default app. */
        const val ANDROID_DEFAULT_APP_NAME = "[DEFAULT]"
    }
}

internal fun FIRMultiFactorInfo.toCompat(): MultiFactorInfo = when (this) {
    is FIRPhoneMultiFactorInfo -> PhoneMultiFactorInfo(UID(), displayName(), enrollmentDate().toTimestamp(), phoneNumber(), this)
    else -> TotpMultiFactorInfo(UID(), displayName(), enrollmentDate().toTimestamp(), this)
}

internal actual fun nativePhoneCredential(verificationId: String, smsCode: String): Any = FIRPhoneAuthProvider.provider().credentialWithVerificationID(verificationId, smsCode)

internal actual fun nativeVerifyPhoneNumber(options: PhoneAuthOptions) {
    val provider = FIRPhoneAuthProvider.providerWithAuth(options.firebaseAuth.ios)
    val delegate = options.verificationContext as? FIRAuthUIDelegateProtocol
    val session = (options.multiFactorSession as? MultiFactorSessionImpl)?.native as? FIRMultiFactorSession
    val hint = options.multiFactorHint?.native as? FIRPhoneMultiFactorInfo
    val completion: (String?, NSError?) -> Unit = { verificationId, error ->
        if (error != null) options.callbacks.onVerificationFailed(error.toAuthException(options.firebaseAuth)) else options.codeSent(verificationId.orEmpty())
    }
    when {
        hint != null -> provider.verifyPhoneNumberWithMultiFactorInfo(hint, delegate, session, completion)
        session != null -> provider.verifyPhoneNumber(options.phoneNumber.orEmpty(), delegate, session, completion)
        else -> provider.verifyPhoneNumber(options.phoneNumber.orEmpty(), delegate, completion)
    }
}

internal actual fun nativePhoneMultiFactorAssertion(credential: PhoneAuthCredential): Any = FIRPhoneMultiFactorGenerator.assertionWithCredential(credential.ios as FIRPhoneAuthCredential)

internal actual fun nativeTotpGenerateSecret(session: MultiFactorSession): Task<TotpSecret> = task(null) { completion ->
    val impl = session as MultiFactorSessionImpl
    FIRTOTPMultiFactorGenerator.generateSecretWithMultiFactorSession(impl.native as FIRMultiFactorSession) { secret, error -> completion(secret?.let { TotpSecretImpl(it, impl.auth) }, error) }
}

internal actual fun nativeTotpAssertionForEnrollment(secret: TotpSecret, oneTimePassword: String): Any = FIRTOTPMultiFactorGenerator.assertionForEnrollmentWithSecret((secret as TotpSecretImpl).ios, oneTimePassword)

internal actual fun nativeTotpAssertionForSignIn(enrollmentId: String, oneTimePassword: String): Any = FIRTOTPMultiFactorGenerator.assertionForSignInWithEnrollmentID(enrollmentId, oneTimePassword)

/** Sets the delegate that presents the reCAPTCHA of the verification when the SDK needs one; the default presents it over the key window. */
public fun PhoneAuthOptions.Builder.setUIDelegate(delegate: FIRAuthUIDelegateProtocol?): PhoneAuthOptions.Builder = apply { verificationContext = delegate }
