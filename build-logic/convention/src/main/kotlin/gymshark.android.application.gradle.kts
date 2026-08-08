import com.android.build.api.dsl.ApplicationExtension
import com.gymshark.buildlogic.SdkConfig
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

// :app only — Hilt root and NavDisplay wiring, no business logic
// (docs/ARCHITECTURE.md §3). Not shared with library modules because it
// configures ApplicationExtension rather than LibraryExtension.

plugins {
    id("com.android.application")
    // No org.jetbrains.kotlin.android: AGP 9 has built-in Kotlin support
    // and rejects the standalone plugin (developer.android.com/build/
    // migrate-to-built-in-kotlin).
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("de.mannodermaus.android-junit5")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<ApplicationExtension> {
    compileSdk = SdkConfig.COMPILE_SDK

    defaultConfig {
        applicationId = "com.gymshark.catalogue"
        minSdk = SdkConfig.MIN_SDK
        targetSdk = SdkConfig.TARGET_SDK
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
    }

    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

extensions.configure<KotlinAndroidProjectExtension> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    val bom = libs.findLibrary("compose-bom").get()
    add("implementation", platform(bom))
    add("androidTestImplementation", platform(bom))

    add("implementation", libs.findLibrary("androidx-core-ktx").get())
    add("implementation", libs.findLibrary("androidx-activity-compose").get())
    add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
    add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
    add("implementation", libs.findLibrary("compose-ui").get())
    add("implementation", libs.findLibrary("compose-ui-graphics").get())
    add("implementation", libs.findLibrary("compose-material3").get())
    add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
    add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())

    add("implementation", libs.findLibrary("navigation3-runtime").get())
    add("implementation", libs.findLibrary("navigation3-ui").get())
    add("implementation", libs.findLibrary("lifecycle-viewmodel-navigation3").get())

    add("implementation", libs.findLibrary("hilt-android").get())
    add("ksp", libs.findLibrary("hilt-compiler").get())

    add("implementation", libs.findLibrary("androidx-profileinstaller").get())

    add("testImplementation", libs.findLibrary("junit5-jupiter-api").get())
    add("testRuntimeOnly", libs.findLibrary("junit5-jupiter-engine").get())
    add("androidTestImplementation", libs.findLibrary("androidx-test-junit").get())
    add("androidTestImplementation", libs.findLibrary("compose-ui-test-junit4").get())
    add("debugImplementation", libs.findLibrary("compose-ui-test-manifest").get())
}
