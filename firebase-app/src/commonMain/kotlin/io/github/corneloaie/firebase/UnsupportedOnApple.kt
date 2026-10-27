/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase

/**
 * Marks a member of the `com.google.firebase` layer that the Firebase Apple SDKs do not provide: on Apple platforms it
 * does nothing (or fails, where it must return a value), as its documentation says. Like [UnsupportedOnJs], the marker
 * is on the Apple actual only, so the warning appears when code is compiled for an Apple target, in common code
 * included; opt in with `@OptIn(UnsupportedOnApple::class)` where the Apple behaviour is acceptable.
 */
@RequiresOptIn(
    message = "Not provided by the Firebase Apple SDKs: on Apple platforms this member does nothing, or fails where it must return a value (see its documentation). " +
        "Opt in with @OptIn(UnsupportedOnApple::class) if that is acceptable.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.CONSTRUCTOR)
public annotation class UnsupportedOnApple
