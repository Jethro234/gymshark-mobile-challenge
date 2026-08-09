plugins {
    id("gymshark.jvm.library")
    alias(libs.plugins.kover)
}

kotlin {
    explicitApi()
}
