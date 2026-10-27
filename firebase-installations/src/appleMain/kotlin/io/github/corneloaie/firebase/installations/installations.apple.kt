package io.github.corneloaie.firebase.installations

import swiftPMImport.io.github.corneloaie.firebase.installations.FIRInstallations

/** The underlying Firebase iOS SDK object. */
public val FirebaseInstallations.ios: FIRInstallations get() = compat.ios
