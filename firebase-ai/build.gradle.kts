import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest
import compat.registerAndroidSourceCompat
import utils.TargetPlatform
import utils.applyFirebaseHierarchy
import utils.stripHeaderStubs
import utils.supportsApple
import utils.toTargetPlatforms

/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

val supportedPlatforms = (project.property("firebase-ai.supportedTargets") as String).toTargetPlatforms()

plugins {
    id("com.android.kotlin.multiplatform.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("testOptionsConvention")
    alias(libs.plugins.publish)
}

kotlin {
    explicitApi()
    applyFirebaseHierarchy()

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        // The module's own code uses the preview API (Live API, templates, on-device inference) it declares.
        optIn.add("com.google.firebase.ai.type.PublicPreviewAPI")
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
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
            namespace = "io.github.corneloaie.firebase.ai"

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

    // No JVM target: firebase-java-sdk (0.6.3) has no AI Logic.

    if (supportedPlatforms.contains(TargetPlatform.Ios)) {
        iosArm64()
        iosX64()
        iosSimulatorArm64()
    }
    if (supportedPlatforms.contains(TargetPlatform.Tvos)) {
        tvosArm64()
        tvosSimulatorArm64()
    }
    if (supportedPlatforms.contains(TargetPlatform.Macos)) {
        macosArm64()
    }
    if (supportedPlatforms.supportsApple()) {
        // The Firebase AI Logic iOS SDK (FirebaseAILogic) is Swift-only, which Kotlin/Native cannot import, so the Apple
        // targets import the local Swift package under apple/, which wraps it in an Objective-C API (the workaround
        // JetBrains recommends for Swift-only packages). The package depends on firebase-ios-sdk; the shared
        // Package.resolved keeps it at the version the other modules import.
        swiftPMDependencies {
            discoverClangModulesImplicitly = false
            iosMinimumDeploymentTarget.set(libs.versions.ios.deploymentTarget.get())
            tvosMinimumDeploymentTarget.set(libs.versions.tvos.deploymentTarget.get())
            macosMinimumDeploymentTarget.set(libs.versions.macos.deploymentTarget.get())
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            localSwiftPackage(
                directory = layout.projectDirectory.dir("apple/FirebaseAILogicObjC"),
                products = listOf("FirebaseAILogicObjC"),
            )
        }
    }

    if (supportedPlatforms.contains(TargetPlatform.WasmJs)) {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
        wasmJs {
            nodejs()
            browser {
                testTask {
                    useKarma {
                        useChromeHeadless()
                    }
                }
            }
        }
    }

    if (supportedPlatforms.contains(TargetPlatform.Js)) {
        js(IR) {
            useCommonJs()
            nodejs {
                testTask {
                    useKarma {
                        useChromeHeadless()
                    }
                }
            }
            browser {
                testTask {
                    useKarma {
                        useChromeHeadless()
                    }
                }
            }
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                this.apiVersion = libs.versions.settings.api.get()
                this.languageVersion = libs.versions.settings.language.get()
                progressiveMode = true
                if (name.lowercase().contains("wasm")) {
                    optIn("kotlin.js.ExperimentalWasmJsInterop")
                }
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
                // The Android SDK's API exposes Flow, JsonElement/JsonObject (function calls) and KSerializer (JsonSchema).
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.json)
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
                    api(libs.google.firebase.ai)
                }
            }
        }

        // JS and wasmJs run the Firebase JS SDK's `firebase/ai`, Apple the Firebase AI Logic iOS SDK; requests and
        // responses cross to them as the JSON of the REST API (see nonAndroidMain). Kotlin/Wasm has no `dynamic`: the JS
        // SDK's values are read and built with firebase-common's interop helpers (json, jsGet, jsonParse, ...). The
        // `firebase` npm package comes through firebase-common on js and wasmJs alike.
        if (supportedPlatforms.contains(TargetPlatform.Js)) {
            getByName("jsMain") {
                dependencies {
                    implementation(project(":firebase-common"))
                }
            }
        }
        if (supportedPlatforms.contains(TargetPlatform.WasmJs)) {
            getByName("wasmJsMain") {
                dependencies {
                    implementation(project(":firebase-common"))
                }
            }
        }
    }
}

// The com.google.firebase.ai classes are header stubs on Android, verified against the AI Logic SDK and stripped; on the
// other platforms they are the module's own classes over the platform SDK (there is no JVM target).
stripHeaderStubs(
    packageDirs = listOf("com/google/firebase/ai"),
    androidReferenceJars = files({
        configurations.getByName("androidCompileClasspath").incoming.artifactView {
            attributes.attribute(Attribute.of("artifactType", String::class.java), "android-classes-jar")
        }.files
    }),
    jvmReferenceJars = files(),
    jvmStubs = false,
)

// firebase-android-sdk keeps AI Logic under ai-logic/; the api.txt is saved as firebase-ai.api.txt.
registerAndroidSourceCompat("ai-logic/firebase-ai/api.txt")

if (supportedPlatforms.supportsApple()) {
    tasks.create<Exec>("launchIosSimulator") {
        commandLine("open", "-a", "Simulator")
    }

    tasks.withType<KotlinNativeSimulatorTest>().configureEach {
        dependsOn("launchIosSimulator")
        standalone.set(false)
        device.set("booted")
    }
}

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = project.group.toString(),
        artifactId = "firebase-ai",
        version = project.version.toString()
    )
}
