/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.pnv

/**
 * The reasons of a [VerificationSupportResult] ([VerificationSupportResult.getReason]), mirroring the Android SDK's
 * `IntDef` annotation `com.google.firebase.pnv.VerificationSupportStatus`.
 *
 * Plain common code: on Android it is the SDK class.
 */
@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
public annotation class VerificationSupportStatus {
    public companion object {
        /** Whether the SIM supports verification is not known. */
        public const val CAPABILITY_STATUS_UNSPECIFIED: Int = 0

        /** The SIM supports verification. */
        public const val CAPABLE: Int = 1

        /** The SIM's carrier does not support verification. */
        public const val INCAPABLE_DUE_TO_CARRIER_UNSUPPORTED: Int = 2

        /** The device's Android version does not support verification. */
        public const val INCAPABLE_DUE_TO_ANDROID_VERSION: Int = 3

        /** The state of the SIM does not allow verification. */
        public const val INCAPABLE_DUE_TO_SIM_STATE: Int = 4
    }
}
