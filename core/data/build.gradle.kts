plugins {
    id("gymshark.android.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    explicitApi()
}

android {
    namespace = "com.gymshark.catalogue.core.data"
}

dependencies {
    implementation(project(":core:model"))
    testImplementation(project(":core:testing"))

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
