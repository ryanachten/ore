plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotless)
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.spotless.plugin)
    implementation(libs.spotbugs.plugin)
}

spotless {
    ratchetFrom("origin/main")

    kotlinGradle {
        ktlint()
    }
}
