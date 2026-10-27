/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package io.github.corneloaie.firebase.analytics

/** The Firebase JS SDK object this wraps. */
public val FirebaseAnalytics.js: io.github.corneloaie.firebase.analytics.externals.FirebaseAnalytics get() = compat.js

/** The Firebase JS SDK object this wraps. */
public val com.google.firebase.analytics.FirebaseAnalytics.js: io.github.corneloaie.firebase.analytics.externals.FirebaseAnalytics get() = native.js
