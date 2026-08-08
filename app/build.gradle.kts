plugins {
    id("gymshark.android.application")
}

android {
    namespace = "com.gymshark.catalogue"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:products"))
}
