plugins {
    id("gymshark.android.macrobenchmark")
}

android {
    namespace = "com.gymshark.catalogue.macrobenchmark"
}

baselineProfile {
    // A real, attached Pixel 9 Pro (docs/PERFORMANCE.md), not a Gradle
    // Managed Device — no `managedDevices { }` block needed.
    useConnectedDevices = true
}
