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

val supportedPlatforms = (project.property("firebase-appdistribution.supportedTargets") as String).toTargetPlatforms()

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("testOptionsConvention")
    alias(libs.plugins.publish)
}

if (supportedPlatforms.contains(TargetPlatform.Android)) {
    android {
        val minSdkVersion: Int by project
        val compileSdkVersion: Int by project

        compileSdk = compileSdkVersion
        namespace = "io.github.corneloaie.firebase.appdistribution"

        defaultConfig {
            minSdk = minSdkVersion
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        testOptions.configureTestOptions(project)
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
        @Suppress("OPT_IN_USAGE")
        androidTarget {
            instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
            unitTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
            publishAllLibraryVariants()
        }
    }

    // firebase-java-sdk has no App Distribution: the JVM actuals are no-op fallbacks (see src/jvmMain).
    if (supportedPlatforms.contains(TargetPlatform.Jvm)) {
        jvm()
    }

    // The iOS SDK builds App Distribution for iOS only (its FirebaseAppDistribution-Beta product depends on the
    // FirebaseAppDistribution target `.when(platforms: [.iOS])`), so there are no macOS or tvOS targets.
    if (supportedPlatforms.contains(TargetPlatform.Ios)) {
        iosArm64()
        iosX64()
        iosSimulatorArm64()
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
                products = listOf(product("FirebaseAppDistribution-Beta")),
                importedClangModules = listOf("FirebaseAppDistribution"),
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
                    // The API-only library: its calls do nothing (and its tasks fail with NOT_IMPLEMENTED) unless the app
                    // also adds the full com.google.firebase:firebase-appdistribution SDK, to pre-release builds only.
                    api(libs.google.firebase.appdistribution.api)
                }
            }
        }
    }
}

// The com.google.* declarations are header stubs on Android (see buildSrc utils/HeaderStubs.kt).
stripHeaderStubs(
    packageDirs = listOf("com/google/firebase"),
    androidReferenceJars = files({
        configurations.findByName("releaseCompileClasspath")?.incoming?.artifactView {
            attributes.attribute(Attribute.of("artifactType", String::class.java), "android-classes-jar")
        }?.files ?: files()
    }),
    jvmReferenceJars = files(),
    // firebase-java-sdk has no App Distribution: the JVM actuals are real code, not stubs.
    jvmStubs = false,
    // Shipped facade: the String forms of the SDK's android.net.Uri members (startFeedback with a screenshot).
    keepClasses = listOf("com/google/firebase/appdistribution/FeedbackScreenshotKt.class"),
)

registerAndroidSourceCompat("firebase-appdistribution-api/api.txt")

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = project.group.toString(),
        artifactId = "firebase-appdistribution",
        version = project.version.toString()
    )
}
