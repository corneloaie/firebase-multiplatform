/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.pnv

/**
 * A verified phone number with its Firebase PNV token, mirroring `com.google.firebase.pnv.VerifiedPhoneNumberTokenResult`;
 * from [FirebasePhoneNumberVerification.getVerifiedPhoneNumber] and
 * [FirebasePhoneNumberVerification.exchangeCredentialResponseForPhoneNumber]. Send [getToken] to your backend, which
 * verifies its signature: this class only reads the claims of the token, without verifying it.
 *
 * Constructing one parses `token`, a JWT. It throws a [FirebasePhoneNumberVerificationException] with
 * [FirebasePnvStatusCodes.INVALID_DIGITAL_CREDENTIAL_RESPONSE] when the token is empty, does not have three parts, or
 * its payload is not a base64url-encoded JSON object. On Android this is the SDK class (also `Parcelable`); the other
 * platforms parse the token the same way.
 */
public expect class VerifiedPhoneNumberTokenResult(token: String) {
    /** The Firebase PNV token: a JWT signed by Firebase, whose `sub` claim is the verified phone number. */
    public fun getToken(): String

    /** When the token expires, its `exp` claim in seconds since the epoch; 0 without one. */
    public fun getExpirationTimestamp(): Long

    /** When the token was issued, its `iat` claim in seconds since the epoch; 0 without one. */
    public fun getIssuedAtTimestamp(): Long

    /** The verified phone number, the token's `sub` claim. */
    public fun getPhoneNumber(): String

    /**
     * Every claim of the token, sorted by name. A JSON string is a `String`, `true` and `false` are a `Boolean`, a
     * number is an `Int`, a `Long` (when it does not fit in an `Int`) or a `Double` (when it has a fraction or an
     * exponent). Nested objects and arrays are `org.json.JSONObject` and `JSONArray` on Android, `Map` and `List`
     * elsewhere; a claim whose value is `null` is `org.json.JSONObject.NULL` on Android and left out elsewhere.
     */
    public fun getClaims(): Map<String, Any>

    /** The nonce of the request, the token's `nonce` claim. */
    public fun getNonce(): String

    /** The Android SDK class's companion object, which holds its `Parcelable` `CREATOR` there; no members here. */
    public companion object
}
