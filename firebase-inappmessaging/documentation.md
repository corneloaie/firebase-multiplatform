# Module firebase-inappmessaging
This module is a direct forward of the Firebase In-App Messaging library. It displays the in-app messages of the campaigns created in the Firebase console, with the SDK's default UI or a display component of your own, and reports the impressions, clicks, dismissals and display errors to listeners (`com.google.firebase.inappmessaging`) or flows (`io.github.corneloaie.firebase.inappmessaging`).

On Apple platforms it wraps the iOS SDK, which has In-App Messaging on iOS and tvOS (with no default UI on tvOS) but not on macOS. firebase-java-sdk has no In-App Messaging, so on the JVM nothing is ever displayed. There is no web SDK.
