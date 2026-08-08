plugins {
    id("gymshark.android.feature")
}

android {
    namespace = "com.gymshark.catalogue.feature.products"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))

    testImplementation(project(":core:testing"))
}
