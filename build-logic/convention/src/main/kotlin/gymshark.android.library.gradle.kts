import com.android.build.api.dsl.LibraryExtension
import com.gymshark.buildlogic.SdkConfig
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

plugins {
    id("com.android.library")
    // No org.jetbrains.kotlin.android: AGP 9 has built-in Kotlin support
    // and rejects the standalone plugin (developer.android.com/build/
    // migrate-to-built-in-kotlin).
    id("de.mannodermaus.android-junit5")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<LibraryExtension> {
    compileSdk = SdkConfig.COMPILE_SDK

    defaultConfig {
        minSdk = SdkConfig.MIN_SDK
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        // No baseline file — docs/CONVENTIONS.md §3: "a baseline is a promise
        // to fix things later, and on a new repository there is no 'later'."
        // These two checks are disabled outright rather than baselined — see
        // gymshark.android.application.gradle.kts for why (deliberately
        // pinned Kotlin/JUnit versions that these checks would otherwise
        // flag forever).
        disable += setOf("GradleDependency", "NewerVersionAvailable")
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
    // Several modules have no test source yet at this point in the build order
    // (docs/openspec/changes/build-product-catalogue/tasks.md); an empty module
    // must not fail the aggregate `test` task.
    failOnNoDiscoveredTests = false
}

dependencies {
    add("implementation", libs.findLibrary("androidx-core-ktx").get())
    add("testImplementation", libs.findLibrary("junit5-jupiter-api").get())
    add("testRuntimeOnly", libs.findLibrary("junit5-jupiter-engine").get())
    add("testImplementation", libs.findLibrary("junit5-jupiter-params").get())
    add("testImplementation", libs.findLibrary("kotlin-test-junit5").get())
}
