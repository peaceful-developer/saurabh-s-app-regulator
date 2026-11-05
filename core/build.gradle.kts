// Pure Kotlin module: it cannot see android.* at all, which enforces the
// "no Android imports in core" rule at compile time (see DECISIONS #20).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
