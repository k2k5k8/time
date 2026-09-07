plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// The project may live on an ExFAT external disk. macOS writes AppleDouble
// `._*` sidecars there, which Android's resource merger mistakes for folders.
// Keep generated Gradle outputs on the internal APFS disk in that case.
if (rootProject.projectDir.absolutePath.startsWith("/Volumes/")) {
    allprojects {
        val safePath = path.replace(':', '_').ifBlank { "root" }
        layout.buildDirectory.set(
            file("${System.getProperty("user.home")}/.gradle/momentmark-build/$safePath"),
        )
    }
}
