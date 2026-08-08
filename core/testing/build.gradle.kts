plugins {
    id("gymshark.android.library")
}

kotlin {
    explicitApi()
}

android {
    namespace = "com.gymshark.catalogue.core.testing"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:data"))

    api(libs.junit5.jupiter.api)
    api(libs.kotlin.test.junit5)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
    api(libs.okhttp.mockwebserver)
}
