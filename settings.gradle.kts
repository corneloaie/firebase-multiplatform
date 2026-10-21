include(
    "firebase-analytics",
    "firebase-app",
    "firebase-appcheck",
    "firebase-appdistribution",
    "firebase-auth",
    "firebase-common",
    "firebase-common-internal",
    "firebase-config",
    "firebase-crashlytics",
    "firebase-database",
    "firebase-dataconnect",
    "firebase-firestore",
    "firebase-functions",
    "firebase-inappmessaging",
    "firebase-installations",
    "firebase-messaging",
    "firebase-perf",
    "firebase-pnv",
    "firebase-storage",
    "test-utils"
)

pluginManagement {
    includeBuild("convention-plugin-test-option")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://plugins.gradle.org/m2/") }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}
