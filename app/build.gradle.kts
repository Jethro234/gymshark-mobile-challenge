plugins {
    id("gymshark.android.application")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.gymshark.catalogue"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:products"))

    implementation(libs.okhttp.core)
    implementation(libs.kotlinx.coroutines.core)
}
