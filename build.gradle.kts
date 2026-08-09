// Every plugin used anywhere in the project is declared here, unapplied,
// so its classes resolve and load exactly once at the root classloader.
// Without this, subprojects that each `alias(...)` the same Kotlin-family
// plugin independently trigger Gradle's "Kotlin Gradle plugin was loaded
// multiple times" warning. Actual application happens per module, either
// through a build-logic convention plugin (see docs/CONVENTIONS.md §2) or
// directly where a module needs one convention plugins don't cover.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.mannodermaus.android.junit5) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    // Applied for real (not apply false) — Kover is only ever used here, at the root, to
    // aggregate coverage from the two pure-Kotlin/data modules named in ARCHITECTURE.md
    // §9.5. It is not applied to every subproject the way ktlint/detekt are below.
    alias(libs.plugins.kover)
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

// docs/ARCHITECTURE.md §9.5 — Kover reported for :core:model and :core:data only, no
// enforced threshold. Compose UI and DI wiring are excluded: coverage there measures
// framework glue, not the logic this suite is trying to prove correct.
dependencies {
    kover(project(":core:model"))
    kover(project(":core:data"))
}

// ktlint and detekt apply to every module uniformly (docs/CONVENTIONS.md
// §3) rather than through a convention plugin, since neither needs
// per-module-type configuration the way the Android/Kotlin conventions do.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        // No baseline, no disabled rules — docs/CONVENTIONS.md §3: ktlint
        // is "never argued about, because it isn't configurable enough to
        // argue about."
    }

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        // Added alongside :core:designsystem (docs/CONVENTIONS.md §3), the
        // first Compose-heavy module — not speculatively during scaffolding.
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    }
}
