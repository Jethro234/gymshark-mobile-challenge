plugins {
    id("gymshark.android.application")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.gymshark.catalogue"

    defaultConfig {
        // Hilt needs its own instrumentation runner so androidTest runs against
        // HiltTestApplication instead of the real GymsharkApplication.
        testInstrumentationRunner = "com.gymshark.catalogue.CustomTestRunner"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:products"))

    implementation(libs.okhttp.core)
    implementation(libs.kotlinx.coroutines.core)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.test.runner)
    kspAndroidTest(libs.hilt.compiler)
}
