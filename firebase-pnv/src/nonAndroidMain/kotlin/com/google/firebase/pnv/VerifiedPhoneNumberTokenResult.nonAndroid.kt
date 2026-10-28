/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.pnv

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/*
 * The Android SDK class, which reads the claims of the token with org.json, ported: the same claims, the same
 * exceptions and messages, and org.json's number types. Nested values are Kotlin collections rather than org.json's.
 */

public actual class VerifiedPhoneNumberTokenResult actual constructor(private val token: String) {
    private val claims: Map<String, Any> = parseClaims(token)

    public actual fun getToken(): String = token

    public actual fun getExpirationTimestamp(): Long = (claims["exp"] as? Number)?.toLong() ?: 0

    public actual fun getIssuedAtTimestamp(): Long = (claims["iat"] as? Number)?.toLong() ?: 0

    public actual fun getPhoneNumber(): String = claims["sub"] as String

    public actual fun getClaims(): Map<String, Any> = claims

    public actual fun getNonce(): String = claims["nonce"] as String

    public actual companion object
}

@OptIn(ExperimentalEncodingApi::class)
private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

/** The claims of the JWT [token], sorted by name (the SDK keeps them in a `TreeMap`). */
@OptIn(ExperimentalEncodingApi::class)
private fun parseClaims(token: String): Map<String, Any> {
    if (token.isEmpty()) throw invalidToken("Token is null or empty", null)
    val parts = token.split(".")
    if (parts.size != 3) throw invalidToken("Invalid JWT format", null)
    // A malformed payload throws IllegalArgumentException, which kotlinx.serialization's SerializationException extends.
    val payload = try {
        Json.parseToJsonElement(base64Url.decode(parts[1]).decodeToString()) as? JsonObject
            ?: throw IllegalArgumentException("The JWT payload is not a JSON object")
    } catch (e: IllegalArgumentException) {
        throw invalidToken("Invalid JWT or parsing error", e)
    }
    return buildMap {
        for ((name, value) in payload.entries.sortedBy { it.key }) {
            value.toClaim()?.let { put(name, it) }
        }
    }
}

private fun invalidToken(message: String, cause: Throwable?) = FirebasePhoneNumberVerificationException(FirebasePnvStatusCodes.INVALID_DIGITAL_CREDENTIAL_RESPONSE, message, cause)

/** The value org.json reads for this element, with Kotlin collections for objects and arrays, and null for `null`. */
private fun JsonElement.toClaim(): Any? = when (this) {
    JsonNull -> null

    is JsonObject -> mapValues { (_, value) -> value.toClaim() }

    is JsonArray -> map { it.toClaim() }

    is JsonPrimitive -> when {
        isString -> content
        else -> booleanOrNull ?: content.toIntOrNull() ?: content.toLongOrNull() ?: content.toDouble()
    }
}
