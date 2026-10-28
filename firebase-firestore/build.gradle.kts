import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import compat.registerAndroidSourceCompat
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import utils.TargetPlatform
import utils.applyFirebaseHierarchy
import utils.stripHeaderStubs
import utils.supportsApple
import utils.toTargetPlatforms

/*
 * Copyright (c) 2020 GitLive Ltd.  Use of this source code is governed by the Apache 2.0 license.
 */

val supportedPlatforms = (project.property("firebase-firestore.supportedTargets") as String).toTargetPlatforms()

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
        freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
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
            namespace = "io.github.corneloaie.firebase.firestore"

            withJava()
            withHostTestBuilder { sourceSetTreeName = KotlinSourceSetTree.test.name }.configure { configureTestOptions(project) }
            withDeviceTestBuilder { sourceSetTreeName = KotlinSourceSetTree.test.name }.configure { configureTestOptions(project) }
            packaging {
                resources.pickFirsts.add("META-INF/kotlinx-serialization-core.kotlin_module")
                resources.pickFirsts.add("META-INF/AL2.0")
                resources.pickFirsts.add("META-INF/LGPL2.1")
                resources.pickFirsts.add("androidsupportmultidexversion.txt")
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
                products = listOf(product("FirebaseFirestore")),
                importedClangModules = listOf("FirebaseFirestoreInternal"),
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
                        // Explicitly specify Mocha here since it seems to be throwing random errors otherwise
                        useMocha {
                            timeout = "180s"
                        }
                    }
                }
            }
            browser {
                testTask {
                    useKarma {
                        useChromeHeadless()
                        // Explicitly specify Mocha here since it seems to be throwing random errors otherwise
                        useMocha {
                            timeout = "180s"
                        }
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
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlinx.serialization.InternalSerializationApi")
                optIn("kotlinx.serialization.ExperimentalSerializationApi")
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
                api(project(":firebase-common"))
                implementation(project(":firebase-common-internal"))
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
                    api(libs.google.firebase.firestore)
                }
            }
        }

        if (supportedPlatforms.contains(TargetPlatform.Jvm)) {
            getByName("jvmMain") {
                kotlin.srcDir("src/androidMain/kotlin")
            }
            project.extensions.getByType<SourceSetContainer>().getByName("jvmMain").java.srcDir("src/androidMain/java")
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

stripHeaderStubs(
    packageDirs = listOf("com/google/firebase"),
    androidReferenceJars = files({
        configurations.getByName("androidCompileClasspath").incoming.artifactView {
            attributes.attribute(Attribute.of("artifactType", String::class.java), "android-classes-jar")
        }.files
    }),
    jvmReferenceJars = files({ configurations.findByName("jvmCompileClasspath")?.files ?: files() }),
    // Shipped: the static members this module's own code reaches through FirestoreStatics.java.
    keepClasses = listOf(
        "com/google/firebase/firestore/FirestoreInternalsKt.class",
        "com/google/firebase/firestore/FirestoreStatics.class",
    ),
    // firebase-java-sdk ports an older Android SDK without the cache-only listen source, the gRPC flow control window
    // setting, the restricted FieldPath.fromDotSeparatedPath, vector values, the minimum and maximum transforms, the
    // BSON types (BSON binary data is a Blob subtype) and the pipeline API.
    jvmMissingMembers = listOf(
        "com/google/firebase/firestore/pipeline/",
        "com/google/firebase/firestore/Pipeline",
        "com/google/firebase/firestore/Pipeline\$ExecuteOptions",
        "com/google/firebase/firestore/Pipeline\$ExecuteOptions\$IndexMode",
        "com/google/firebase/firestore/Pipeline\$Snapshot",
        "com/google/firebase/firestore/PipelineResult",
        "com/google/firebase/firestore/PipelineSource",
        "com/google/firebase/firestore/FirebaseFirestore.pipeline()Lcom/google/firebase/firestore/PipelineSource;",
        "com/google/firebase/firestore/ListenSource",
        "com/google/firebase/firestore/SnapshotListenOptions",
        "com/google/firebase/firestore/SnapshotListenOptions\$Builder",
        "com/google/firebase/firestore/Query.addSnapshotListener(Lcom/google/firebase/firestore/SnapshotListenOptions;Lcom/google/firebase/firestore/EventListener;)Lcom/google/firebase/firestore/ListenerRegistration;",
        "com/google/firebase/firestore/DocumentReference.addSnapshotListener(Lcom/google/firebase/firestore/SnapshotListenOptions;Lcom/google/firebase/firestore/EventListener;)Lcom/google/firebase/firestore/ListenerRegistration;",
        "com/google/firebase/firestore/FirebaseFirestoreSettings.getGrpcFlowControlWindow()I",
        "com/google/firebase/firestore/FirebaseFirestoreSettings.DEFAULT_GRPC_FLOW_CONTROL_WINDOW",
        "com/google/firebase/firestore/FirebaseFirestoreSettings\$Builder.getGrpcFlowControlWindow()I",
        "com/google/firebase/firestore/FirebaseFirestoreSettings\$Builder.setGrpcFlowControlWindow(I)V",
        "com/google/firebase/firestore/FirebaseFirestoreSettings\$Builder.setGrpcFlowControlWindow(I)Lcom/google/firebase/firestore/FirebaseFirestoreSettings\$Builder;",
        "com/google/firebase/firestore/FieldPath.fromDotSeparatedPath(Ljava/lang/String;)Lcom/google/firebase/firestore/FieldPath;",
        "com/google/firebase/firestore/VectorValue",
        "com/google/firebase/firestore/FieldValue.vector([D)Lcom/google/firebase/firestore/VectorValue;",
        "com/google/firebase/firestore/FieldValue.minimum(J)Lcom/google/firebase/firestore/FieldValue;",
        "com/google/firebase/firestore/FieldValue.minimum(D)Lcom/google/firebase/firestore/FieldValue;",
        "com/google/firebase/firestore/FieldValue.maximum(J)Lcom/google/firebase/firestore/FieldValue;",
        "com/google/firebase/firestore/FieldValue.maximum(D)Lcom/google/firebase/firestore/FieldValue;",
        "com/google/firebase/firestore/DocumentSnapshot.getVectorValue(Ljava/lang/String;)Lcom/google/firebase/firestore/VectorValue;",
        "com/google/firebase/firestore/BsonObjectId",
        "com/google/firebase/firestore/BsonTimestamp",
        "com/google/firebase/firestore/Decimal128Value",
        "com/google/firebase/firestore/Int32Value",
        "com/google/firebase/firestore/MaxKey",
        "com/google/firebase/firestore/MinKey",
        "com/google/firebase/firestore/RegexValue",
        "com/google/firebase/firestore/Blob.getSubType()I",
        "com/google/firebase/firestore/Blob.createBsonBinary([B)Lcom/google/firebase/firestore/Blob;",
        "com/google/firebase/firestore/Blob.createBsonBinary(I[B)Lcom/google/firebase/firestore/Blob;",
        "com/google/firebase/firestore/DocumentSnapshot.getBsonObjectId(Ljava/lang/String;)Lcom/google/firebase/firestore/BsonObjectId;",
        "com/google/firebase/firestore/DocumentSnapshot.getBsonTimestamp(Ljava/lang/String;)Lcom/google/firebase/firestore/BsonTimestamp;",
        "com/google/firebase/firestore/DocumentSnapshot.getDecimal128Value(Ljava/lang/String;)Lcom/google/firebase/firestore/Decimal128Value;",
        "com/google/firebase/firestore/DocumentSnapshot.getInt32Value(Ljava/lang/String;)Lcom/google/firebase/firestore/Int32Value;",
        "com/google/firebase/firestore/DocumentSnapshot.getMaxKey(Ljava/lang/String;)Lcom/google/firebase/firestore/MaxKey;",
        "com/google/firebase/firestore/DocumentSnapshot.getMinKey(Ljava/lang/String;)Lcom/google/firebase/firestore/MinKey;",
        "com/google/firebase/firestore/DocumentSnapshot.getRegexValue(Ljava/lang/String;)Lcom/google/firebase/firestore/RegexValue;",
    ),
)

registerAndroidSourceCompat("firebase-firestore/api.txt")

mavenPublishing {
    publishToMavenCentral(automaticRelease = true)
    signAllPublications()

    coordinates(
        groupId = project.group.toString(),
        artifactId = "firebase-firestore",
        version = project.version.toString()
    )
}
