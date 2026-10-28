import org.gradle.kotlin.dsl.kotlin
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
 * Copyright (c) 2023 GitLive Ltd. Use of this source code is governed by the Apache 2.0 license.
 */

val supportedPlatforms = (project.property("firebase-storage.supportedTargets") as String).toTargetPlatforms()

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
            namespace = "io.github.corneloaie.firebase.storage"

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
                products = listOf(product("FirebaseStorage")),
                importedClangModules = listOf("FirebaseStorage"),
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
                }
            }
        }

        getByName("commonMain") {
            dependencies {
                api(project(":firebase-app"))
                api(project(":firebase-common"))
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
                    api(libs.google.firebase.storage)
                }
            }
        }

        if (supportedPlatforms.contains(TargetPlatform.WasmJs)) {
            getByName("wasmJsMain") {
                dependencies {
                    implementation(libs.kotlinx.browser)
                }
            }
        }

        if (supportedPlatforms.contains(TargetPlatform.Jvm)) {
            getByName("jvmMain") {
                kotlin.srcDir("src/androidMain/kotlin")
            }
        }
    }
}

stripHeaderStubs(
    packageDirs = listOf("com/google/firebase"),
    androidReferenceJars = files({
        configurations.getByName("androidCompileClasspath").incoming.artifactView {
            attributes.attribute(Attribute.of("artifactType", String::class.java), "android-classes-jar")
        }.files
    }),
    jvmReferenceJars = files({ configurations.findByName("jvmCompileClasspath")?.files ?: files() }),
    // Shipped facades: the Any / String forms of the SDK's android.net.Uri and java.io.File members, for common code.
    keepClasses = listOf(
        "com/google/firebase/storage/StorageFilesKt.class",
        "com/google/firebase/storage/StorageDownloadsKt.class",
        "com/google/firebase/storage/StorageTaskUriKt.class",
    ),
)

registerAndroidSourceCompat("firebase-storage/api.txt")

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = project.group.toString(),
        artifactId = "firebase-storage",
        version = project.version.toString()
    )
}
