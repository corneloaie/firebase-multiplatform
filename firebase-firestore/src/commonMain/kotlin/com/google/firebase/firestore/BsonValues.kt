/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.firestore

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/*
 * The BSON types of the Android SDK's firebase-firestore (MongoDB compatibility), as plain common code: on Android the
 * classes are header stubs that are stripped, so the SDK's own bind; elsewhere they are converted to and from the iOS
 * and JS SDKs' BSON types where values cross into or out of the SDK. firebase-java-sdk has none of them, so they are
 * not available on the JVM (see `jvmMissingMembers` in the build file). BSON binary data is a [Blob] with a subtype.
 */

/**
 * A BSON ObjectId, mirroring `com.google.firebase.firestore.BsonObjectId` from the Firebase Android SDK.
 *
 * @property value The 24-character hex string of the ObjectId.
 */
public class BsonObjectId(@JvmField public val value: String) {
    override fun equals(other: Any?): Boolean = this === other || (other is BsonObjectId && other.value == value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "BsonObjectId{value='$value'}"
}

/**
 * A BSON Timestamp, mirroring `com.google.firebase.firestore.BsonTimestamp` from the Firebase Android SDK.
 *
 * @property seconds The seconds since the Unix epoch, an unsigned 32-bit integer.
 * @property increment The ordinal of the operation within the second, an unsigned 32-bit integer.
 * @throws IllegalArgumentException if [seconds] or [increment] is not an unsigned 32-bit integer.
 */
public class BsonTimestamp(@JvmField public val seconds: Long, @JvmField public val increment: Long) {
    init {
        require(seconds in 0..UInt.MAX_VALUE.toLong()) { "The field 'seconds' value ($seconds) does not represent an unsigned 32-bit integer." }
        require(increment in 0..UInt.MAX_VALUE.toLong()) { "The field 'increment' value ($increment) does not represent an unsigned 32-bit integer." }
    }

    override fun equals(other: Any?): Boolean = this === other || (other is BsonTimestamp && other.seconds == seconds && other.increment == increment)

    override fun hashCode(): Int = (31 * seconds + increment).toInt()

    override fun toString(): String = "BsonTimestampValue{seconds=$seconds, increment=$increment}"
}

/**
 * A 128-bit decimal, mirroring `com.google.firebase.firestore.Decimal128Value` from the Firebase Android SDK. Values are
 * equal when they are numerically equal (`1.0` and `1`, `0` and `-0`, `NaN` and `NaN`), as on Android, where the SDK
 * compares them as 128-bit binary floating-point numbers; here they are compared exactly as decimals, which differs
 * only for values beyond that precision or range.
 *
 * @property stringValue The decimal as written: `NaN`, `Infinity`, `+Infinity`, `-Infinity` or a number such as
 *   `-1.5e-3` (`[+-]?[0-9]*(\.[0-9]*)?([eE][+-]?[0-9]+)?` with at least one digit).
 * @throws NumberFormatException if [stringValue] is not a decimal.
 */
public class Decimal128Value(@JvmField public val stringValue: String) {
    /** [stringValue] in a canonical form for equality, see [canonical]. */
    private val canonicalValue: String = canonical(stringValue)

    override fun equals(other: Any?): Boolean = this === other || (other is Decimal128Value && other.canonicalValue == canonicalValue)

    override fun hashCode(): Int = canonicalValue.hashCode()

    override fun toString(): String = "Decimal128Value{value=$stringValue}"

    /**
     * [value] parsed as the Android SDK parses a decimal string: `NaN`, `+Infinity`, `-Infinity`, `0` for both zeros, or
     * the sign, the significant digits without leading or trailing zeros and the exponent (`+15e-1` for `1.50`).
     */
    private fun canonical(value: String): String {
        when (value) {
            "NaN" -> return value
            "Infinity", "+Infinity" -> return "+Infinity"
            "-Infinity" -> return value
        }
        var index = 0
        val negative = value.startsWith('-')
        if (negative || value.startsWith('+')) index++
        val digits = StringBuilder()
        while (index < value.length && value[index] in '0'..'9') digits.append(value[index++])
        var exponent = 0L
        if (index < value.length && value[index] == '.') {
            val fractionStart = ++index
            while (index < value.length && value[index] in '0'..'9') digits.append(value[index++])
            exponent -= index - fractionStart
        }
        if (index < value.length && (value[index] == 'e' || value[index] == 'E')) {
            index++
            val exponentSign = if (index < value.length && value[index] == '-') -1 else 1
            if (index < value.length && (value[index] == '-' || value[index] == '+')) index++
            val exponentStart = index
            var written = 0L
            while (index < value.length && value[index] in '0'..'9') {
                written = 10 * written + (value[index++] - '0')
                if (index - exponentStart > 9) throw NumberFormatException("Exponent too large $value")
            }
            if (index == exponentStart) throw NumberFormatException("Invalid number $value")
            exponent += exponentSign * written
        }
        if (digits.isEmpty() || index != value.length) throw NumberFormatException("Invalid number $value")
        val significant = digits.trimStart('0')
        if (significant.isEmpty()) return "0"
        val trimmed = significant.trimEnd('0')
        return "${if (negative) '-' else '+'}${trimmed}e${exponent + significant.length - trimmed.length}"
    }
}

/**
 * A 32-bit integer, stored as such rather than as Firestore's 64-bit integer, mirroring
 * `com.google.firebase.firestore.Int32Value` from the Firebase Android SDK.
 *
 * @property value The integer.
 */
public class Int32Value(@JvmField public val value: Int) {
    override fun equals(other: Any?): Boolean = this === other || (other is Int32Value && other.value == value)

    override fun hashCode(): Int = value

    override fun toString(): String = "Int32Value{value=$value}"
}

/** The BSON MaxKey, which orders after every other value, mirroring `com.google.firebase.firestore.MaxKey` from the Firebase Android SDK. */
public class MaxKey private constructor() {
    public companion object {
        private val INSTANCE = MaxKey()

        /** The only [MaxKey]. */
        @JvmStatic
        public fun instance(): MaxKey = INSTANCE
    }
}

/** The BSON MinKey, which orders before every other value, mirroring `com.google.firebase.firestore.MinKey` from the Firebase Android SDK. */
public class MinKey private constructor() {
    public companion object {
        private val INSTANCE = MinKey()

        /** The only [MinKey]. */
        @JvmStatic
        public fun instance(): MinKey = INSTANCE
    }
}

/**
 * A BSON regular expression, mirroring `com.google.firebase.firestore.RegexValue` from the Firebase Android SDK.
 *
 * @property pattern The regular expression.
 * @property options The options (flags) of the regular expression, such as `i`.
 */
public class RegexValue(@JvmField public val pattern: String, @JvmField public val options: String) {
    override fun equals(other: Any?): Boolean = this === other || (other is RegexValue && other.pattern == pattern && other.options == options)

    override fun hashCode(): Int = 31 * pattern.hashCode() + options.hashCode()

    override fun toString(): String = "RegexValue{pattern='$pattern', options='$options'}"
}
