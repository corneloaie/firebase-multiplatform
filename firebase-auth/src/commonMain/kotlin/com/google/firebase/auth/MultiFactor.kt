/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.auth

import com.google.android.gms.tasks.Task

/**
 * The multi-factor enrollment of a user, mirroring `com.google.firebase.auth.MultiFactor`: enrolls a second factor from
 * a [MultiFactorAssertion] and lists or removes the enrolled ones.
 */
public expect abstract class MultiFactor() {
    /** The enrolled second factors. */
    public abstract val enrolledFactors: List<MultiFactorInfo>

    /** Enrolls the factor of [assertion] under [displayName]; requires a recent sign-in. */
    public abstract fun enroll(assertion: MultiFactorAssertion, displayName: String?): Task<Nothing?>

    /** A session for enrolling a factor, passed to the factor's verification. */
    public abstract fun getSession(): Task<MultiFactorSession>

    /** Removes the enrolled [info]. */
    public abstract fun unenroll(info: MultiFactorInfo): Task<Nothing?>

    /** Removes the enrolled factor with [factorUid]. */
    public abstract fun unenroll(factorUid: String): Task<Nothing?>
}

/** A verified second factor to enroll or sign in with, mirroring `com.google.firebase.auth.MultiFactorAssertion`. */
public expect abstract class MultiFactorAssertion() {
    /** The type of factor, e.g. [PhoneMultiFactorGenerator.FACTOR_ID]. */
    public abstract val factorId: String
}

/**
 * An enrolled second factor, mirroring `com.google.firebase.auth.MultiFactorInfo`. Its JSON form is Android-only.
 */
public expect abstract class MultiFactorInfo() {
    public abstract val displayName: String?

    /** When the factor was enrolled, in milliseconds since the epoch. */
    public abstract val enrollmentTimestamp: Long

    /** The type of factor, e.g. [PhoneMultiFactorGenerator.FACTOR_ID]. */
    public abstract val factorId: String
    public abstract val uid: String

    public companion object {
        public val FACTOR_ID_KEY: String
    }
}

/** An enrolled phone factor, mirroring `com.google.firebase.auth.PhoneMultiFactorInfo`. */
public expect class PhoneMultiFactorInfo(uid: String, displayName: String?, enrollmentTimestamp: Long, phoneNumber: String) : MultiFactorInfo {
    public val phoneNumber: String
}

/** An enrolled TOTP (authenticator app) factor, mirroring `com.google.firebase.auth.TotpMultiFactorInfo`. */
public expect class TotpMultiFactorInfo : MultiFactorInfo

/**
 * A session for enrolling a second factor or signing in with one, mirroring `com.google.firebase.auth.MultiFactorSession`.
 */
public expect abstract class MultiFactorSession()

/**
 * Completes a sign-in that requires a second factor, mirroring `com.google.firebase.auth.MultiFactorResolver`; from
 * [FirebaseAuthMultiFactorException.resolver].
 */
public expect abstract class MultiFactorResolver() {
    public abstract val firebaseAuth: FirebaseAuth

    /** The factors the user can sign in with. */
    public abstract val hints: List<MultiFactorInfo>

    /** The session to verify one of the [hints] with. */
    public abstract val session: MultiFactorSession

    /** Completes the sign-in with the verified [assertion]. */
    public abstract fun resolveSignIn(assertion: MultiFactorAssertion): Task<AuthResult>
}

/** A verified phone factor, mirroring `com.google.firebase.auth.PhoneMultiFactorAssertion`. */
public expect class PhoneMultiFactorAssertion(credential: PhoneAuthCredential) : MultiFactorAssertion

/** Creates phone factor assertions, mirroring `com.google.firebase.auth.PhoneMultiFactorGenerator`. */
public expect class PhoneMultiFactorGenerator() {
    public companion object {
        public val FACTOR_ID: String

        /** An assertion for the verified phone [credential]. */
        public fun getAssertion(credential: PhoneAuthCredential): PhoneMultiFactorAssertion
    }
}

/**
 * A verified TOTP factor, mirroring `com.google.firebase.auth.TotpMultiFactorAssertion`; usually created with
 * [TotpMultiFactorGenerator.getAssertionForEnrollment] or [TotpMultiFactorGenerator.getAssertionForSignIn], which call
 * this constructor with the [secret] or the [enrollmentId] respectively.
 *
 * The Apple and JS SDKs only create TOTP assertions through their TOTP generators, so on those platforms the SDK's
 * assertion is made by its generator when this one is first used: for enrolling [secret] when it is set, otherwise for
 * signing in with [enrollmentId]; with neither, using the assertion fails with `IllegalArgumentException`.
 *
 * @param oneTimePassword The one-time password the authenticator app shows; it must not be empty.
 * @param secret The secret being enrolled, from [TotpMultiFactorGenerator.generateSecret].
 * @param enrollmentId The [MultiFactorInfo.uid] of the enrolled factor to sign in with.
 */
public expect class TotpMultiFactorAssertion(oneTimePassword: String, secret: TotpSecret?, enrollmentId: String?) : MultiFactorAssertion

/** Creates TOTP factor secrets and assertions, mirroring `com.google.firebase.auth.TotpMultiFactorGenerator`. */
public expect class TotpMultiFactorGenerator {
    public companion object {
        public val FACTOR_ID: String

        /** Generates a secret to enroll an authenticator app with, in [session] from [MultiFactor.getSession]. */
        public fun generateSecret(session: MultiFactorSession): Task<TotpSecret>

        /** An assertion for enrolling [secret], from the [oneTimePassword] the authenticator app shows. */
        public fun getAssertionForEnrollment(secret: TotpSecret, oneTimePassword: String): TotpMultiFactorAssertion

        /** An assertion for signing in with the enrolled factor [enrollmentId], from the [oneTimePassword] the authenticator app shows. */
        public fun getAssertionForSignIn(enrollmentId: String, oneTimePassword: String): TotpMultiFactorAssertion
    }
}

/**
 * A TOTP secret to enroll an authenticator app with, mirroring `com.google.firebase.auth.TotpSecret`.
 *
 * The iOS SDK exposes only the shared secret, the QR code URL and the OTP app hand-off: on Apple platforms the hash
 * algorithm and the code length are read from the QR code URL, and [codeIntervalSeconds], [enrollmentCompletionDeadline]
 * and [sessionInfo] throw `UnsupportedOperationException`. The JS SDK keeps the session private, so [sessionInfo] throws
 * there too.
 */
public expect interface TotpSecret {
    /** The shared secret, to enter in the authenticator app by hand. */
    public val sharedSecretKey: String

    /** A URL of a QR code the authenticator app can scan, for [accountName] at [issuer]. */
    public fun generateQrCodeUrl(accountName: String, issuer: String): String

    /**
     * A URL of a QR code the authenticator app can scan, for the signed-in user's email at the app's name, as
     * `generateQrCodeUrl(email, app.name)`. It fails without a signed-in user with an email, except on JS and Wasm,
     * where the JS SDK names the account `unknownuser` then.
     */
    public fun generateQrCodeUrl(): String

    /** Opens [qrCodeUrl] in the authenticator app. */
    public fun openInOtpApp(qrCodeUrl: String)

    /** How often the one-time passwords change, in seconds. Throws `UnsupportedOperationException` on Apple platforms. */
    public val codeIntervalSeconds: Int

    /** The number of digits of the one-time passwords. */
    public val codeLength: Int

    /**
     * When the enrollment has to be completed by, in seconds since the epoch. Throws `UnsupportedOperationException` on
     * Apple platforms.
     */
    public val enrollmentCompletionDeadline: Long

    /** The hash algorithm of the one-time passwords, such as `SHA1`. */
    public val hashAlgorithm: String

    /**
     * The session of the enrollment, which the SDK sends back with the one-time password. Android only: throws
     * `UnsupportedOperationException` on Apple platforms, JS and Wasm, whose SDKs keep it private.
     */
    public val sessionInfo: String
}
