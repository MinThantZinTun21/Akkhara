// All keyboard behaviour lives here. This module must never depend on Android
// (see docs/adr/0003-custom-view-keyboard-not-compose.md).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
