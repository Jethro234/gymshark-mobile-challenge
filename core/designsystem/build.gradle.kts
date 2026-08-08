plugins {
    id("gymshark.android.library.compose")
}

kotlin {
    explicitApi()
}

android {
    namespace = "com.gymshark.catalogue.core.designsystem"
}

dependencies {
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
