plugins {
    id("java")
}

group = "com.ryanachten"
version = "0.0.1-SNAPSHOT"

val libs = versionCatalogs.named("libs")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
	mavenCentral()
}

dependencies {
	testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
