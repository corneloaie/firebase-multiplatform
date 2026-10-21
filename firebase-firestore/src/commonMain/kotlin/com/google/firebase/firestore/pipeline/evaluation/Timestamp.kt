/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.firestore.pipeline.evaluation

/*
 * The range checks of the timestamps the pipeline functions accept, which the Android SDK's evaluation package makes
 * public: from 0001-01-01T00:00:00Z to 9999-12-31T23:59:59.999999999Z.
 */

private const val MIN_SECONDS = -62135596800L
private const val MAX_SECONDS = 253402300799L

/** Whether a timestamp of [seconds] since the epoch and [nanos] nanoseconds is in the range of Firestore timestamps. */
public fun isTimestampInBounds(seconds: Long, nanos: Int): Boolean = seconds in MIN_SECONDS..MAX_SECONDS && nanos in 0 until 1_000_000_000

/** Whether [microseconds] since the epoch are in the range of Firestore timestamps. */
public fun isMicrosecondsInTimestampBounds(microseconds: Long): Boolean = microseconds in MIN_SECONDS * 1_000_000..MAX_SECONDS * 1_000_000 + 999_999

/** Whether [milliseconds] since the epoch are in the range of Firestore timestamps. */
public fun isMillisecondsInTimestampBounds(milliseconds: Long): Boolean = milliseconds in MIN_SECONDS * 1_000..MAX_SECONDS * 1_000 + 999

/** Whether [seconds] since the epoch are in the range of Firestore timestamps. */
public fun isSecondsInTimestampBounds(seconds: Long): Boolean = seconds in MIN_SECONDS..MAX_SECONDS
