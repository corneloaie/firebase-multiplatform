# Module firebase-appdistribution
This module is a direct forward of the Firebase App Distribution library. It provides the in-app tester API, like checking for and installing new pre-release builds.

On Android it depends on the API-only `firebase-appdistribution-api`, whose calls do nothing until the app adds the full `com.google.firebase:firebase-appdistribution` SDK to its pre-release build variants. On iOS it uses the iOS SDK, which has no in-app feedback and installs a new release by opening its download link; macOS and tvOS have no App Distribution SDK, and on the JVM it does nothing.
