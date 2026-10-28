import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import compat.registerAndroidSourceCompat
import utils.TargetPlatform
import utils.applyFirebaseHierarchy
import utils.stripHeaderStubs
import utils.supportsApple
import utils.toTargetPlatforms

/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

val supportedPlatforms = (project.property("firebase-inappmessaging.supportedTargets") as String).toTargetPlatforms()

plugins {
    id("com.android.kotlin.multiplatform.library")
    kotlin("multiplatform")
    id("testOptionsConvention")
    alias(libs.plugins.publish)
}

kotlin {
    explicitApi()
    applyFirebaseHierarchy()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    if (this is KotlinJvmCompilerOptions) {
                        jvmTarget = JvmTarget.JVM_17
                    }
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }

    if (supportedPlatforms.contains(TargetPlatform.Android)) {
        android {
            val minSdkVersion: Int by project
            val compileSdkVersion: Int by project

            compileSdk = compileSdkVersion
            minSdk = minSdkVersion
            namespace = "io.github.corneloaie.firebase.inappmessaging"

            withHostTestBuilder { sourceSetTreeName = KotlinSourceSetTree.test.name }.configure { configureTestOptions(project) }
            withDeviceTestBuilder { sourceSetTreeName = KotlinSourceSetTree.test.name }.configure { configureTestOptions(project) }
            packaging {
                resources.pickFirsts.add("META-INF/kotlinx-serialization-core.kotlin_module")
                resources.pickFirsts.add("META-INF/AL2.0")
                resources.pickFirsts.add("META-INF/LGPL2.1")
            }
            lint {
                abortOnError = false
            }
        }
    }

    // firebase-java-sdk has no In-App Messaging: the JVM gets no-op fallbacks (src/jvmMain), so that common code
    // using In-App Messaging still compiles for a desktop target.
    if (supportedPlatforms.contains(TargetPlatform.Jvm)) {
        jvm()
    }

    if (supportedPlatforms.contains(TargetPlatform.Ios)) {
        iosArm64()
        iosX64()
        iosSimulatorArm64()
    }
    if (supportedPlatforms.contains(TargetPlatform.Tvos)) {
        tvosArm64()
        tvosSimulatorArm64()
    }
    // No macOS: the iOS SDK declares In-App Messaging API_UNAVAILABLE(macos), and its SwiftPM product
    // (FirebaseInAppMessaging-Beta) only builds the module for iOS and tvOS.
    if (supportedPlatforms.supportsApple()) {
        swiftPMDependencies {
            discoverClangModulesImplicitly = false
            iosMinimumDeploymentTarget.set(libs.versions.ios.deploymentTarget.get())
            tvosMinimumDeploymentTarget.set(libs.versions.tvos.deploymentTarget.get())
            macosMinimumDeploymentTarget.set(libs.versions.macos.deploymentTarget.get())
            // The Objective-C API is the FirebaseInAppMessagingInternal module, which the product's Swift module
            // (FirebaseInAppMessaging, SwiftUI helpers only) re-exports.
            swiftPackage(
                url = url("https://github.com/firebase/firebase-ios-sdk.git"),
                version = from(libs.versions.firebase.ios.sdk.get()),
                products = listOf(product("FirebaseInAppMessaging-Beta")),
                importedClangModules = listOf("FirebaseInAppMessagingInternal"),
            )
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                this.apiVersion = libs.versions.settings.api.get()
                this.languageVersion = libs.versions.settings.language.get()
                progressiveMode = true
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                if (name.lowercase().contains("ios")
                    || name.lowercase().contains("apple")
                    || name.lowercase().contains("tvos")
                    || name.lowercase().contains("macos")
                    ) {
                    optIn("kotlinx.cinterop.ExperimentalForeignApi")
                    optIn("kotlinx.cinterop.BetaInteropApi")
                }
            }
        }

        getByName("commonMain") {
            dependencies {
                api(project(":firebase-app"))
                implementation(project(":firebase-common"))
            }
        }

        getByName("commonTest") {
            dependencies {
                implementation(project(":test-utils"))
            }
        }

        if (supportedPlatforms.contains(TargetPlatform.Android)) {
            getByName("androidMain") {
                dependencies {
                    api(libs.google.firebase.inappmessaging)
                    // The default message UI, and the com.google.firebase.inappmessaging.display API.
                    api(libs.google.firebase.inappmessaging.display)
                }
            }
        }
    }
}

// The com.google.* declarations are header stubs on Android (see buildSrc utils/HeaderStubs.kt).
stripHeaderStubs(
    packageDirs = listOf("com/google/firebase"),
    androidReferenceJars = files({
        configurations.getByName("androidCompileClasspath").incoming.artifactView {
            attributes.attribute(Attribute.of("artifactType", String::class.java), "android-classes-jar")
        }.files
    }),
    jvmReferenceJars = files(),
    // firebase-java-sdk has no In-App Messaging: the JVM actuals are real (no-op) code, not stubs.
    jvmStubs = false,
)

registerAndroidSourceCompat("firebase-inappmessaging/api.txt", "firebase-inappmessaging-display/api.txt")

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = project.group.toString(),
        artifactId = "firebase-inappmessaging",
        version = project.version.toString()
    )
}
