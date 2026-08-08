import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

plugins {
    id("gymshark.android.library.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("implementation", libs.findLibrary("hilt-android").get())
    add("ksp", libs.findLibrary("hilt-compiler").get())
    add("implementation", libs.findLibrary("hilt-navigation-compose").get())

    add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-ktx").get())
    add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())

    add("implementation", libs.findLibrary("navigation3-runtime").get())
    add("implementation", libs.findLibrary("navigation3-ui").get())
    add("implementation", libs.findLibrary("lifecycle-viewmodel-navigation3").get())

    add("implementation", libs.findLibrary("kotlinx-collections-immutable").get())

    add("testImplementation", libs.findLibrary("turbine").get())
    add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())

    add("androidTestImplementation", libs.findLibrary("compose-ui-test-junit4").get())
    add("androidTestImplementation", libs.findLibrary("hilt-android-testing").get())
    add("kspAndroidTest", libs.findLibrary("hilt-compiler").get())
}
