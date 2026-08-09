import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsTask
import org.gradle.api.plugins.quality.Checkstyle

plugins {
    id("java")
    id("checkstyle")
    id("com.github.spotbugs")
    id("com.diffplug.spotless")
}

group = "com.ryanachten"
version = "0.0.1-SNAPSHOT"

val libs = versionCatalogs.named("libs")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
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

checkstyle {
    // Google's own advice: pin the config to the exact tool version used.
    toolVersion = libs.findVersion("checkstyle").get().requiredVersion
    configDirectory.set(rootProject.layout.projectDirectory.dir("config/checkstyle"))
    configProperties.put("org.checkstyle.google.severity", "error")
    // Hook up the suppression filters declared in google_checks.xml. The config
    // references these via placeholders; without absolute paths the relative
    // defaults resolve against the checkstyle working dir and the files are
    // silently skipped (optional=true).
    configProperties.put(
        "org.checkstyle.google.suppressionfilter.config",
        rootProject.file("config/checkstyle/checkstyle-suppressions.xml").absolutePath)
    configProperties.put(
        "org.checkstyle.google.suppressionxpathfilter.config",
        rootProject.file("config/checkstyle/checkstyle-xpath-suppressions.xml").absolutePath)
    maxWarnings = 0
    maxErrors = 0
}

tasks.withType<Checkstyle>().configureEach {
    reports {
        xml.required = true
        html.required = true
    }
}

spotbugs {
    toolVersion = libs.findVersion("spotbugs").get().requiredVersion
    ignoreFailures = false
    effort = Effort.MAX
    reportLevel = Confidence.MEDIUM
    excludeFilter.set(rootProject.layout.projectDirectory.file("config/spotbugs/exclude.xml"))
}

tasks.withType<SpotBugsTask>().configureEach {
    reports.maybeCreate("html").required.set(true)
    reports.maybeCreate("xml").required.set(true)
}

spotless {
    ratchetFrom("origin/main")

    format("misc") {
        target("*.gradle", "*.gradle.kts")
        trimTrailingWhitespace()
        leadingSpacesToTabs()
        endWithNewline()
    }
    java {
        googleJavaFormat("1.27.0").reflowLongStrings()
        formatAnnotations()
    }
}

tasks.named("check") {
    dependsOn("spotlessCheck")
}
