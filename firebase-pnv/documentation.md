# Module firebase-pnv
This module is a direct forward of the Firebase Phone Number Verification library (`com.google.firebase:firebase-pnv`). It gets the phone number of a SIM in the device directly from its carrier, with a token signed by Firebase for the app's backend to verify, instead of sending an SMS code.

Phone Number Verification exists on Android only: Firebase has no iOS, web or Java SDK for it. So that common code using it compiles everywhere, the module also has iOS, macOS, tvOS, JS and Wasm targets. There every task of `FirebasePhoneNumberVerification` fails with `UnsupportedOperationException`, `enableTestSession` does nothing, and the members carry `@UnsupportedOnApple` or `@UnsupportedOnJs`. The value types work on every platform: `VerificationSupportResult`, `FirebasePnvStatusCodes`, the exception, and `VerifiedPhoneNumberTokenResult`, which reads the claims of a token as the Android SDK does. There is no JVM target, as firebase-java-sdk has no Phone Number Verification either.

On Android, `getVerifiedPhoneNumber` needs an Activity; common code passes it as `Any`. Apps compile against SDK 35 or newer, which the SDK's androidx.core 1.16 dependency requires.
