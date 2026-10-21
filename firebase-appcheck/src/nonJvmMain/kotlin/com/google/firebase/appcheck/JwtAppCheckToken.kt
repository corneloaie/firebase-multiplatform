/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * An [AppCheckToken] known by its JWT alone, which is all the JS SDK and the iOS SDK's token notification report. Its
 * expiry is the JWT's `exp` claim, as the Android SDK reads it; `0` for a token that is not a JWT.
 */
internal class JwtAppCheckToken(private val jwt: String) : AppCheckToken() {
    private val expiry = jwtExpireTimeMillis(jwt)

    override fun getToken(): String = jwt

    override fun getExpireTimeMillis(): Long = expiry

    override fun toString(): String = "AppCheckToken(expireTimeMillis=$expireTimeMillis)"
}

private val expClaim = Regex(""""exp"\s*:\s*(\d+)""")

@OptIn(ExperimentalEncodingApi::class)
private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

/** The `exp` claim of [jwt] in milliseconds since the epoch, or `0` when [jwt] is not a JWT with one. */
@OptIn(ExperimentalEncodingApi::class)
internal fun jwtExpireTimeMillis(jwt: String): Long {
    val payload = jwt.split('.').takeIf { it.size == 3 }?.get(1) ?: return 0
    val claims = try {
        base64Url.decode(payload).decodeToString()
    } catch (e: IllegalArgumentException) {
        return 0
    }
    return expClaim.find(claims)?.groupValues?.get(1)?.toLongOrNull()?.times(1000) ?: 0
}
