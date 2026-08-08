plugins {
    `kotlin-dsl`
}

group = "com.gymshark.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // implementation, not compileOnly: precompiled script plugins apply()
    // these by id at runtime, so the implementation jars must be on the
    // classpath of whatever consumes this plugin — compileOnly only
    // resolves types while compiling the script itself.
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.kotlin.compose.compiler.gradlePlugin)
    implementation(libs.ksp.gradlePlugin)
    implementation(libs.hilt.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.mannodermaus.gradlePlugin)
    implementation(libs.kover.gradlePlugin)
    implementation(libs.baselineprofile.gradlePlugin)
}

// Precompiled script plugins under src/main/kotlin/*.gradle.kts register
// themselves automatically, with plugin id equal to the filename. No
// explicit `gradlePlugin { plugins { ... } }` block is needed for that.
