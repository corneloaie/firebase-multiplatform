package io.github.corneloaie.firebase.installations

import io.github.corneloaie.firebase.installations.externals.Installations

/** The underlying Firebase JS SDK object. */
public val FirebaseInstallations.js: Installations get() = compat.js
