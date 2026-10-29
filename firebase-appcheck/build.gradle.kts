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

val supportedPlatforms = (project.property("firebase-appcheck.supportedTargets") as String).toTargetPlatforms()

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
                        // AppCheckProvider.getLimitedUseToken() is a Java default method in the Android SDK. Without
                        // this, the header stub of the interface would also get an AppCheckProvider$DefaultImpls class,
                        // which the real SDK does not have.
                        freeCompilerArgs.add("-jvm-default=no-compatibility")
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
            namespace = "io.github.corneloaie.firebase.appcheck"

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

    // No JVM target: firebase-java-sdk 0.6.3 contains the App Check core classes (its Firestore and Database depend on
    // them) but does not register the App Check component, so FirebaseAppCheck.getInstance() returns null there, and
    // shipping this module's own JVM classes would put duplicates of the java-sdk classes on the classpath.

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
        swiftPMDependencies {
            discoverClangModulesImplicitly = false
            iosMinimumDeploymentTarget.set(libs.versions.ios.deploymentTarget.get())
            tvosMinimumDeploymentTarget.set(libs.versions.tvos.deploymentTarget.get())
            macosMinimumDeploymentTarget.set(libs.versions.macos.deploymentTarget.get())
            swiftPackage(
                url = url("https://github.com/firebase/firebase-ios-sdk.git"),
                version = from(libs.versions.firebase.ios.sdk.get()),
                products = listOf(product("FirebaseAppCheck")),
                importedClangModules = listOf("FirebaseAppCheck"),
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
                    api(libs.google.firebase.appcheck)
                    // The provider factories of the com.google.firebase.appcheck.{debug,playintegrity,recaptcha}
                    // packages are part of the common API, so their Android SDK artifacts are api dependencies too.
                    api(libs.google.firebase.appcheck.debug)
                    api(libs.google.firebase.appcheck.playintegrity)
                    api(libs.google.firebase.appcheck.recaptcha)
                }
            }
        }
    }
}

// The com.google.* declarations are header stubs on Android (see buildSrc utils/HeaderStubs.kt); there is no JVM target.
stripHeaderStubs(
    packageDirs = listOf("com/google/firebase"),
    androidReferenceJars = files({
        configurations.getByName("androidCompileClasspath").incoming.artifactView {
            attributes.attribute(Attribute.of("artifactType", String::class.java), "android-classes-jar")
        }.files
    }),
    jvmReferenceJars = files(),
    jvmStubs = false,
)

// firebase-android-sdk keeps the App Check modules under appcheck/; each api.txt is saved as <module directory>.api.txt.
registerAndroidSourceCompat(
    "appcheck/firebase-appcheck/api.txt",
    "appcheck/firebase-appcheck-debug/api.txt",
    "appcheck/firebase-appcheck-playintegrity/api.txt",
    "appcheck/firebase-appcheck-recaptcha/api.txt",
)

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
        artifactId = "firebase-appcheck",
        version = project.version.toString()
    )
}
