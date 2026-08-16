plugins {
    id("gymshark.android.application")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.gymshark.catalogue"

    defaultConfig {
        // Hilt needs its own instrumentation runner so androidTest runs against
        // HiltTestApplication instead of the real GymsharkApplication.
        testInstrumentationRunner = "com.gymshark.catalogue.CustomTestRunner"
    }

    buildTypes {
        release {
            // R8 full mode is AGP 9's default (docs/PERFORMANCE.md §6) — enabling it is just
            // this flag, no separate android.enableR8.fullMode property needed.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed with the debug key so :macrobenchmark can install and run this variant
            // on a local device without a production keystore (docs/PERFORMANCE.md §2).
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:products"))

    implementation(libs.okhttp.core)
    implementation(libs.kotlinx.coroutines.core)
    // GymsharkApplication.newImageLoader — :core:designsystem depends on Coil via
    // `implementation`, not `api`, so :app needs its own direct dependency to build a shared
    // ImageLoader off the same OkHttpClient Retrofit uses. coil-compose (not just
    // coil-network-okhttp) is needed for SingletonImageLoader itself.
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    androidTestImplementation(project(":core:testing"))
    androidTestImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.androidx.test.runner)
    kspAndroidTest(libs.hilt.compiler)

    constraints {
        androidTestImplementation(libs.androidx.test.espresso.core) {
            because(
                "Compose's ui-test-junit4 pulls espresso-core in transitively to inject touch " +
                    "events. 3.5.0 does that by reflecting on InputManager.getInstance(), removed " +
                    "in Android 15+, so every instrumented test fails on API 35+ with " +
                    "NoSuchMethodException. Constraint only — no test here uses Espresso directly.",
            )
        }
    }

    // The macrobenchmark module only ever runs against :app's release build (measuring the
    // real, shrunk artefact), so its Baseline Profile generator output feeds straight back in.
    baselineProfile(project(":macrobenchmark"))
}
