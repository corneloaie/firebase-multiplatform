/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.auth

import com.google.android.gms.tasks.Task

public actual abstract class MultiFactor actual constructor() {
    public actual abstract val enrolledFactors: List<MultiFactorInfo>
    public actual abstract fun enroll(assertion: MultiFactorAssertion, displayName: String?): Task<Nothing?>
    public actual abstract fun getSession(): Task<MultiFactorSession>
    public actual abstract fun unenroll(info: MultiFactorInfo): Task<Nothing?>
    public actual abstract fun unenroll(factorUid: String): Task<Nothing?>
}

/** @property native The platform SDK's assertion. */
public actual abstract class MultiFactorAssertion actual constructor() {
    public actual abstract val factorId: String
    internal abstract val native: Any
}

/** @property native The platform SDK's info, when the SDK produced it. */
public actual abstract class MultiFactorInfo actual constructor() {
    public actual abstract val displayName: String?
    public actual abstract val enrollmentTimestamp: Long
    public actual abstract val factorId: String
    public actual abstract val uid: String
    internal abstract val native: Any?

    public actual companion object {
        public actual val FACTOR_ID_KEY: String = "factorIdKey"
    }
}

public actual class PhoneMultiFactorInfo actual constructor(
    override val uid: String,
    override val displayName: String?,
    override val enrollmentTimestamp: Long,
    public actual val phoneNumber: String,
) : MultiFactorInfo() {
    override val factorId: String get() = PhoneMultiFactorGenerator.FACTOR_ID
    override var native: Any? = null
        internal set

    internal constructor(uid: String, displayName: String?, enrollmentTimestamp: Long, phoneNumber: String, native: Any) : this(uid, displayName, enrollmentTimestamp, phoneNumber) {
        this.native = native
    }
}

public actual class TotpMultiFactorInfo internal constructor(
    override val uid: String,
    override val displayName: String?,
    override val enrollmentTimestamp: Long,
    override val native: Any,
) : MultiFactorInfo() {
    override val factorId: String get() = TotpMultiFactorGenerator.FACTOR_ID
}

public actual abstract class MultiFactorSession actual constructor()

/** @property auth The instance of the session's user, where the platform needs it later (the TOTP secret's defaults on Apple). */
internal class MultiFactorSessionImpl(val native: Any, val auth: FirebaseAuth? = null) : MultiFactorSession()

public actual abstract class MultiFactorResolver actual constructor() {
    public actual abstract val firebaseAuth: FirebaseAuth
    public actual abstract val hints: List<MultiFactorInfo>
    public actual abstract val session: MultiFactorSession
    public actual abstract fun resolveSignIn(assertion: MultiFactorAssertion): Task<AuthResult>
}

/** @property credential The verified phone credential. */
public actual class PhoneMultiFactorAssertion actual constructor(public val credential: PhoneAuthCredential) : MultiFactorAssertion() {
    override val factorId: String get() = PhoneMultiFactorGenerator.FACTOR_ID
    override val native: Any by lazy { nativePhoneMultiFactorAssertion(credential) }
}

public actual class PhoneMultiFactorGenerator actual constructor() {
    public actual companion object {
        public actual val FACTOR_ID: String = "phone"

        public actual fun getAssertion(credential: PhoneAuthCredential): PhoneMultiFactorAssertion = PhoneMultiFactorAssertion(credential)
    }
}

/**
 * The platform SDKs only create TOTP assertions through their generators, so the SDK's assertion is made when this one
 * is first used: for enrolling [secret] when it is set, otherwise for signing in with [enrollmentId].
 */
public actual class TotpMultiFactorAssertion actual constructor(oneTimePassword: String, secret: TotpSecret?, enrollmentId: String?) : MultiFactorAssertion() {
    init {
        // As the Android SDK's constructor checks it.
        require(oneTimePassword.isNotEmpty()) { "Given String is empty or null" }
    }

    override val factorId: String get() = TotpMultiFactorGenerator.FACTOR_ID
    override val native: Any by lazy {
        when {
            secret != null -> nativeTotpAssertionForEnrollment(secret, oneTimePassword)
            enrollmentId != null -> nativeTotpAssertionForSignIn(enrollmentId, oneTimePassword)
            else -> throw IllegalArgumentException("A TOTP assertion needs the secret to enroll or the enrollment ID to sign in with")
        }
    }
}

public actual class TotpMultiFactorGenerator private constructor() {
    public actual companion object {
        public actual val FACTOR_ID: String = "totp"

        public actual fun generateSecret(session: MultiFactorSession): Task<TotpSecret> = nativeTotpGenerateSecret(session)

        public actual fun getAssertionForEnrollment(secret: TotpSecret, oneTimePassword: String): TotpMultiFactorAssertion = TotpMultiFactorAssertion(oneTimePassword, secret, null)

        public actual fun getAssertionForSignIn(enrollmentId: String, oneTimePassword: String): TotpMultiFactorAssertion = TotpMultiFactorAssertion(oneTimePassword, null, enrollmentId)
    }
}
