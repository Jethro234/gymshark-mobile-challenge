plugins {
    id("gymshark.android.feature")
}

android {
    namespace = "com.gymshark.catalogue.feature.products"
}

// docs/PERFORMANCE.md §5 — Compose compiler stability report finding. ErrorCause
// (:core:model) is a closed sealed interface of only `data object`s — every instance is a
// singleton, structurally immutable. The Compose compiler still infers it unstable because
// :core:model doesn't apply the Compose compiler plugin (correctly: it's a plain domain
// module with no UI framework dependency, docs/ARCHITECTURE.md §3), so the compiler can't
// see across that module boundary. compose_compiler_config.conf tells it the truth without
// adding a Compose dependency to :core:model. (The file itself can't hold this comment —
// its one-class-per-line format doesn't support `#` comments.)
composeCompiler {
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_compiler_config.conf"))
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))

    testImplementation(project(":core:testing"))
}
