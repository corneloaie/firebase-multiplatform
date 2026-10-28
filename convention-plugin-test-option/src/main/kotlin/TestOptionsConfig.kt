import com.android.build.api.dsl.KotlinMultiplatformAndroidDeviceTest
import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTest
import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate

/** The device tests' options: SDK, runner, and the managed emulator that CI runs them on. */
fun KotlinMultiplatformAndroidDeviceTest.configureTestOptions(project: Project) {
    val targetSdkVersion: Int by project
    targetSdk { version = release(targetSdkVersion) }
    instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    animationsDisabled = true
    managedDevices.localDevices.create("gradleManagedDevice") {
        device = "Pixel 2"
        apiLevel = 33
        systemImageSource = "google-atd"
        require64Bit = true
    }
}

/** The host (unit) tests' options. */
fun KotlinMultiplatformAndroidHostTest.configureTestOptions(project: Project) {
    val targetSdkVersion: Int by project
    targetSdk { version = release(targetSdkVersion) }
    isIncludeAndroidResources = true
}
