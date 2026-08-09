package com.gymshark.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Wires `-Pcompose.compiler.reports=true` / `-Pcompose.compiler.metrics=true` (docs/
 * PERFORMANCE.md §5) to the Compose compiler's own report/metrics output. Off by default —
 * every module applying `org.jetbrains.kotlin.plugin.compose` calls this the same way, so
 * the flag behaves identically everywhere instead of only on whichever module happened to
 * wire it first.
 */
internal fun Project.configureComposeCompilerReports() {
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        if (project.hasProperty("compose.compiler.reports")) {
            reportsDestination.set(layout.buildDirectory.dir("compose_compiler/reports"))
        }
        if (project.hasProperty("compose.compiler.metrics")) {
            metricsDestination.set(layout.buildDirectory.dir("compose_compiler/metrics"))
        }
    }
}
