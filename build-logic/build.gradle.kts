plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotless)
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.spotless.plugin)
}

spotless {
    ratchetFrom("origin/main")

    kotlinGradle {
        ktlint()
    }
}
