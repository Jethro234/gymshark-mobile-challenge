import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin/JVM modules — currently only :core:model. No Android
// dependency, so its tests are plain JVM tests (docs/ARCHITECTURE.md §3).

plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    add("testImplementation", libs.findLibrary("junit5-jupiter-api").get())
    add("testRuntimeOnly", libs.findLibrary("junit5-jupiter-engine").get())
    add("testImplementation", libs.findLibrary("junit5-jupiter-params").get())
    add("testImplementation", libs.findLibrary("kotlin-test-junit5").get())
}
