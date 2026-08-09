pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "gymshark-mobile-challenge"

include(":app")
include(":core:model")
include(":core:data")
include(":core:designsystem")
include(":core:testing")
include(":feature:products")
// :macrobenchmark is added in task 10.1 (docs/openspec tasks.md group 10),
// not part of the "six modules" foundation (docs/SCOPE.md §2).
include(":macrobenchmark")
