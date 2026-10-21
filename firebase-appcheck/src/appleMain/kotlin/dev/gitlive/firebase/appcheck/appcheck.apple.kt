/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package dev.gitlive.firebase.appcheck

import swiftPMImport.dev.gitlive.firebase.appcheck.FIRAppAttestProviderFactory
import swiftPMImport.dev.gitlive.firebase.appcheck.FIRAppCheck
import swiftPMImport.dev.gitlive.firebase.appcheck.FIRDeviceCheckProviderFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckProvider
import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.IosFirebaseAppCheck
import com.google.firebase.appcheck.createProvider

/** The underlying Firebase iOS SDK object; null when the app had no provider factory when it was configured. */
public val FirebaseAppCheck.ios: FIRAppCheck? get() = (compat as? IosFirebaseAppCheck)?.ios

/**
 * Apple only: the factory of the DeviceCheck provider, the iOS SDK's `DeviceCheckProviderFactory`, which the iOS SDK
 * uses on devices by default. DeviceCheck does not work on simulators.
 *
 * @property ios The underlying Firebase iOS SDK factory, which is installed as it is.
 */
public class DeviceCheckProviderFactory : AppCheckProviderFactory {
    public val ios: FIRDeviceCheckProviderFactory = FIRDeviceCheckProviderFactory()

    /** Returns the DeviceCheck provider of [firebaseApp]. */
    override fun create(firebaseApp: FirebaseApp): AppCheckProvider = ios.createProvider(firebaseApp)

    override fun toString(): String = "DeviceCheckProviderFactory"
}

/**
 * Apple only: the factory of the App Attest provider, the iOS SDK's `AppAttestProviderFactory` (iOS 14, macOS 11.3 and
 * tvOS 15 or later). App Attest needs the App Attest capability and does not work on simulators.
 *
 * @property ios The underlying Firebase iOS SDK factory, which is installed as it is.
 */
public class AppAttestProviderFactory : AppCheckProviderFactory {
    public val ios: FIRAppAttestProviderFactory = FIRAppAttestProviderFactory()

    /** Returns the App Attest provider of [firebaseApp]. */
    override fun create(firebaseApp: FirebaseApp): AppCheckProvider = ios.createProvider(firebaseApp)

    override fun toString(): String = "AppAttestProviderFactory"
}
