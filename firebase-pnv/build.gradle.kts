import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import compat.GeneratedAndroidSdkApi
import compat.registerAndroidSourceCompat
import utils.TargetPlatform
import utils.applyFirebaseHierarchy
import utils.stripHeaderStubs
import utils.toTargetPlatforms

/*
 * Copyright (c) 2026 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

val supportedPlatforms = (project.property("firebase-pnv.supportedTargets") as String).toTargetPlatforms()

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

        // firebase-pnv 17.0.0 depends on androidx.core 1.16, whose AAR metadata requires compileSdk 35.
        compileSdk = maxOf(compileSdkVersion, 35)
        namespace = "dev.gitlive.firebase.pnv"

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

    // Phone Number Verification exists on Android only: Firebase has no iOS, JS or Java SDK for it. The other targets
    // compile the API so that common code using it builds everywhere, and their actuals fail (see documentation.md).
    // No JVM target: firebase-java-sdk has no Phone Number Verification either.

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
    // No swiftPMDependencies of its own: there is no Apple SDK to bind to. The FirebaseCore package that firebase-app
    // declares is inherited, which is all the Apple code (FirebaseApp) and its test binaries need.

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
                }
            }
        }

        getByName("commonMain") {
            dependencies {
                api(project(":firebase-app"))
            }
        }

        getByName("commonTest") {
            dependencies {
                implementation(project(":test-utils"))
            }
        }

        // The JWT claims of VerifiedPhoneNumberTokenResult, which Android gets from the SDK (over org.json).
        getByName("nonAndroidMain") {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
            }
        }

        if (supportedPlatforms.contains(TargetPlatform.Android)) {
            getByName("androidMain") {
                dependencies {
                    api(libs.google.firebase.pnv)
                }
            }
        }
    }
}

// The com.google.* declarations are header stubs on Android (see buildSrc utils/HeaderStubs.kt); there is no JVM target.
stripHeaderStubs(
    packageDirs = listOf("com/google/firebase"),
    androidReferenceJars = files({
        configurations.findByName("releaseCompileClasspath")?.incoming?.artifactView {
            attributes.attribute(Attribute.of("artifactType", String::class.java), "android-classes-jar")
        }?.files ?: files()
    }),
    jvmReferenceJars = files(),
    jvmStubs = false,
    // Shipped facade: the Any form of the SDK's android.content.Context member (getVerifiedPhoneNumber).
    keepClasses = listOf("com/google/firebase/pnv/VerifiedPhoneNumberKt.class"),
)

// firebase-pnv is not open source and publishes no api.txt: it is generated from the SDK's published classes.
registerAndroidSourceCompat(
    generated = listOf(
        GeneratedAndroidSdkApi(
            name = "firebase-pnv",
            artifacts = listOf("com.google.firebase:firebase-pnv"),
            packages = listOf("com/google/firebase/pnv/"),
        ),
    ),
)

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = "dev.gitlive",
        artifactId = "firebase-pnv",
        version = project.version.toString()
    )

    pom {
        name.set("firebase-kotlin-sdk")
        description.set("The Firebase Kotlin SDK is a Kotlin-first SDK for Firebase. It's API is similar to the Firebase Android SDK Kotlin Extensions but also supports multiplatform projects, enabling you to use Firebase directly from your common source targeting iOS, Android or JS.")
        url.set("https://github.com/GitLiveApp/firebase-kotlin-sdk")
        inceptionYear.set("2019")

        scm {
            url.set("https://github.com/GitLiveApp/firebase-kotlin-sdk")
            connection.set("scm:git:https://github.com/GitLiveApp/firebase-kotlin-sdk.git")
            developerConnection.set("scm:git:https://github.com/GitLiveApp/firebase-kotlin-sdk.git")
            tag.set("HEAD")
        }

        issueManagement {
            system.set("GitHub Issues")
            url.set("https://github.com/GitLiveApp/firebase-kotlin-sdk/issues")
        }

        developers {
            developer {
                name.set("Nicholas Bransby-Williams")
                email.set("nbransby@gmail.com")
            }
        }

        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
                comments.set("A business-friendly OSS license")
            }
        }
    }
}
