/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

package com.google.firebase.appcheck

import swiftPMImport.io.github.corneloaie.firebase.app.FIRApp
import swiftPMImport.io.github.corneloaie.firebase.appcheck.FIRAppCheckErrorDomain
import swiftPMImport.io.github.corneloaie.firebase.appcheck.FIRAppCheckProviderFactoryProtocol
import swiftPMImport.io.github.corneloaie.firebase.appcheck.FIRAppCheckProviderProtocol
import swiftPMImport.io.github.corneloaie.firebase.appcheck.FIRAppCheckToken
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import io.github.corneloaie.firebase.appcheck.AppAttestProviderFactory
import io.github.corneloaie.firebase.appcheck.DeviceCheckProviderFactory
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.NSLocalizedDescriptionKey
import platform.Foundation.NSSelectorFromString
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

/** The completion handler of the iOS SDK's token requests. */
internal typealias IosTokenCompletion = (FIRAppCheckToken?, NSError?) -> Unit

/** An [AppCheckToken] of the iOS SDK. */
internal class IosAppCheckToken(val ios: FIRAppCheckToken) : AppCheckToken() {
    override fun getToken(): String = ios.token

    override fun getExpireTimeMillis(): Long = (ios.expirationDate.timeIntervalSince1970 * 1000).toLong()

    override fun toString(): String = "AppCheckToken(expireTimeMillis=$expireTimeMillis)"
}

/** A [Task] completed by an iOS SDK token request that [start] makes. */
internal inline fun iosTokenTask(start: (IosTokenCompletion) -> Unit): Task<AppCheckToken> {
    val source = TaskCompletionSource<AppCheckToken>()
    start { token, error ->
        if (token != null) source.trySetResult(IosAppCheckToken(token)) else source.trySetException(error.toAppCheckException())
    }
    return source.task
}

// FIRAppCheckErrorCode: Unknown = 0, ServerUnreachable = 1, InvalidConfiguration = 2, Keychain = 3, Unsupported = 4
private fun NSError?.toAppCheckException(): FirebaseException {
    val message = this?.localizedDescription ?: "The App Check provider returned neither a token nor an error"
    return if (this?.code == 1L) FirebaseNetworkException(message) else FirebaseException(message)
}

private fun Throwable.toNSError(): NSError = NSError.errorWithDomain(
    FIRAppCheckErrorDomain,
    0,
    mapOf<Any?, Any>(NSLocalizedDescriptionKey to (message ?: toString())),
)

/** A provider the iOS SDK implements, as an [AppCheckProvider]. */
internal class NativeAppCheckProvider(val ios: FIRAppCheckProviderProtocol) : AppCheckProvider {
    override fun getToken(): Task<AppCheckToken> = iosTokenTask { ios.getTokenWithCompletion(it) }

    /** The iOS SDK's providers all implement the optional limited-use request; [getToken] for any other. */
    override fun getLimitedUseToken(): Task<AppCheckToken> = iosTokenTask { completion ->
        if (ios.respondsToSelector(NSSelectorFromString("getLimitedUseTokenWithCompletion:"))) {
            ios.getLimitedUseTokenWithCompletion(completion)
        } else {
            ios.getTokenWithCompletion(completion)
        }
    }
}

/** The provider [create][FIRAppCheckProviderFactoryProtocol.createProviderWithApp]s of an iOS SDK factory for [app]. */
internal fun FIRAppCheckProviderFactoryProtocol.createProvider(app: FirebaseApp): AppCheckProvider = createProviderWithApp(app.ios as objcnames.classes.FIRApp)?.let(::NativeAppCheckProvider)
    ?: unavailableProvider("The iOS SDK has no App Check provider for app ${app.name} here")

/**
 * The iOS SDK form of a factory: the iOS SDK's own factory for the factories it implements, and an adapter for any
 * other, which [AppCheckProviderFactory.create]s the provider when the iOS SDK asks for it.
 */
internal fun AppCheckProviderFactory.toIos(): FIRAppCheckProviderFactoryProtocol = when (this) {
    is DebugAppCheckProviderFactory -> ios
    is DeviceCheckProviderFactory -> ios
    is AppAttestProviderFactory -> ios
    else -> recaptchaFactoryOrNull() ?: KotlinAppCheckProviderFactory(this)
}

/** The iOS SDK's reCAPTCHA factory when this is a [com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory] (iOS only). */
internal expect fun AppCheckProviderFactory.recaptchaFactoryOrNull(): FIRAppCheckProviderFactoryProtocol?

/** Adapts an [AppCheckProviderFactory] of Kotlin code to the iOS SDK. */
private class KotlinAppCheckProviderFactory(private val factory: AppCheckProviderFactory) :
    NSObject(),
    FIRAppCheckProviderFactoryProtocol {
    /** A failure to create the provider fails its token requests rather than crossing into the iOS SDK. */
    override fun createProviderWithApp(app: objcnames.classes.FIRApp): FIRAppCheckProviderProtocol? {
        val provider = try {
            factory.create(FirebaseApp.getInstance((app as FIRApp).name))
        } catch (e: Exception) {
            unavailableProvider("The App Check provider factory failed: ${e.message ?: e}")
        }
        return (provider as? NativeAppCheckProvider)?.ios ?: KotlinAppCheckProvider(provider)
    }
}

/** Adapts an [AppCheckProvider] of Kotlin code to the iOS SDK. */
private class KotlinAppCheckProvider(private val provider: AppCheckProvider) :
    NSObject(),
    FIRAppCheckProviderProtocol {
    override fun getTokenWithCompletion(handler: IosTokenCompletion) = complete(handler) { provider.getToken() }

    override fun getLimitedUseTokenWithCompletion(handler: IosTokenCompletion) = complete(handler) { provider.getLimitedUseToken() }

    private inline fun complete(noinline handler: IosTokenCompletion, request: () -> Task<AppCheckToken>) {
        val task = try {
            request()
        } catch (e: Exception) {
            handler(null, e.toNSError())
            return
        }
        task.addOnCompleteListener { completed ->
            val token = if (completed.isSuccessful) completed.result else null
            when {
                token is IosAppCheckToken -> handler(token.ios, null)
                token != null -> handler(FIRAppCheckToken(token.token, NSDate.dateWithTimeIntervalSince1970(token.expireTimeMillis / 1000.0)), null)
                else -> handler(null, (completed.exception ?: IllegalStateException("The App Check token request was canceled")).toNSError())
            }
        }
    }
}
