import com.android.build.api.dsl.LibraryExtension
import com.gymshark.buildlogic.configureComposeCompilerReports
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

plugins {
    id("gymshark.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

extensions.configure<LibraryExtension> {
    buildFeatures {
        compose = true
    }
}

configureComposeCompilerReports()

dependencies {
    val bom = libs.findLibrary("compose-bom").get()
    add("implementation", platform(bom))
    add("androidTestImplementation", platform(bom))

    add("implementation", libs.findLibrary("compose-ui").get())
    add("implementation", libs.findLibrary("compose-ui-graphics").get())
    add("implementation", libs.findLibrary("compose-ui-text").get())
    add("implementation", libs.findLibrary("compose-foundation").get())
    add("implementation", libs.findLibrary("compose-material3").get())
    // The small, official icon set (~20 icons material3 itself needs internally) — covers
    // the AutoMirrored back arrow the design docs require, without pulling in
    // material-icons-extended for one icon.
    add("implementation", libs.findLibrary("compose-material-icons-core").get())
    add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
    add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
    add("debugImplementation", libs.findLibrary("compose-ui-test-manifest").get())
}
