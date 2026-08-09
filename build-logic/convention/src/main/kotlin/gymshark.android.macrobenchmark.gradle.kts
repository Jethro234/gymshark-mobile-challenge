import com.android.build.api.dsl.TestExtension
import com.gymshark.buildlogic.SdkConfig
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

// :macrobenchmark only — a com.android.test module, not library/application. AGP 9 has
// built-in Kotlin support, same reasoning as every other convention plugin here.
plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<TestExtension> {
    compileSdk = SdkConfig.COMPILE_SDK

    defaultConfig {
        minSdk = SdkConfig.MIN_SDK
        targetSdk = SdkConfig.TARGET_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Benchmarks run against :app's release build — the whole point is measuring the
    // shrunk, optimised artefact users actually get, not a debuggable one. No custom
    // buildTypes block here: applying androidx.baselineprofile to both this module and
    // :app auto-generates and wires the matching benchmarkRelease/nonMinifiedRelease
    // build types on both sides (developer.android.com/topic/performance/baselineprofiles/
    // create-baselineprofile) — hand-rolling a "benchmark" buildType here fights that.
    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

extensions.configure<KotlinAndroidProjectExtension> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    add("implementation", libs.findLibrary("androidx-test-junit").get())
    add("implementation", libs.findLibrary("androidx-test-runner").get())
    add("implementation", libs.findLibrary("androidx-benchmark-macro-junit4").get())
    add("implementation", libs.findLibrary("androidx-uiautomator").get())
}
