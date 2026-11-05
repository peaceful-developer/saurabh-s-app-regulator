// Pure Kotlin module: it cannot see android.* at all, which enforces the
// "no Android imports in core" rule at compile time (see DECISIONS #20).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
