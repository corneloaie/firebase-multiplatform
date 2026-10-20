plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Loaded here (rather than compileOnly) so the helpers in utils/ can use the Kotlin Gradle plugin API at runtime;
    // the Kotlin plugin needs the Android plugin in the same class loader.
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.gradle.plugin)
    implementation(libs.asm) // header stub verification (utils/HeaderStubs.kt)
    // Header stubs: constructor visibility. buildSrc is compiled by Gradle's embedded Kotlin, which cannot read a newer
    // library's metadata; readLenient still reads the classes the project's own Kotlin produces.
    implementation("org.jetbrains.kotlin:kotlin-metadata-jvm:2.1.21")
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
}
