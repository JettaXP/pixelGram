import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

group = "com.pixelgram.desktop"
version = "0.1.0"

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
            "androidx.compose.foundation.layout.ExperimentalLayoutApi",
        )
    }
}

// TDLib native library for the OS we build on. Release builds are made
// per OS in CI, so each package carries only its own native library.
val tdlightNatives: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arm = System.getProperty("os.arch").let { it == "aarch64" || it == "arm64" }
    when {
        os.contains("win") -> "windows_amd64"
        os.contains("mac") -> "macos_arm64"
        arm -> "linux_arm64_gnu_ssl3"
        else -> "linux_amd64_gnu_ssl3"
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.coroutines.swing)
    implementation(libs.material.color.utilities)
    implementation(libs.qrcodegen)

    implementation(platform(libs.tdlight.bom))
    implementation(libs.tdlight.java)
    implementation(group = "it.tdlight", name = "tdlight-natives", classifier = tdlightNatives)
}

// API credentials are compiled in from the environment or gradle.properties.
val generateBuildConfig by tasks.registering {
    // Empty environment variables (unset CI secrets) fall back to gradle.properties.
    val apiId = providers.environmentVariable("TELEGRAM_API_ID").filter { it.isNotBlank() }
        .orElse(providers.gradleProperty("pixelgram.apiId"))
        .orElse("")
    val apiHash = providers.environmentVariable("TELEGRAM_API_HASH").filter { it.isNotBlank() }
        .orElse(providers.gradleProperty("pixelgram.apiHash"))
        .orElse("")
    val appVersion = project.version.toString()
    val outputDir = layout.buildDirectory.dir("generated/buildconfig")
    inputs.property("apiId", apiId)
    inputs.property("apiHash", apiHash)
    inputs.property("version", appVersion)
    outputs.dir(outputDir)
    doLast {
        val id = apiId.get().trim().toIntOrNull() ?: 0
        val file = outputDir.get().file("com/pixelgram/desktop/BuildConfig.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.pixelgram.desktop
            |
            |object BuildConfig {
            |    const val VERSION = "$appVersion"
            |    const val API_ID = $id
            |    const val API_HASH = "${apiHash.get().trim()}"
            |}
            |""".trimMargin()
        )
    }
}

kotlin.sourceSets.named("main") {
    kotlin.srcDir(generateBuildConfig)
}

compose.desktop {
    application {
        mainClass = "com.pixelgram.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi, TargetFormat.Exe)
            packageName = "pixelGram"
            packageVersion = project.version.toString()
            description = "pixelGram - Telegram client with Material 3"
            vendor = "jettax"
            licenseFile.set(project.file("LICENSE"))
            modules("java.sql", "jdk.unsupported", "java.naming")

            linux {
                packageName = "pixelgram"
                iconFile.set(project.file("packaging/icons/pixelgram.png"))
                appCategory = "Network"
                menuGroup = "Network"
            }
            windows {
                iconFile.set(project.file("packaging/icons/pixelgram.ico"))
                menuGroup = "pixelGram"
                perUserInstall = true
                dirChooser = true
                upgradeUuid = "2E4E866B-6267-4C9C-8209-97D2256C270A"
            }
        }
    }
}

// Development: forward -Dpixelgram.* properties to `./gradlew run`.
tasks.withType<JavaExec>().configureEach {
    System.getProperties()
        .filterKeys { it.toString().startsWith("pixelgram.") }
        .forEach { (key, value) -> systemProperty(key.toString(), value) }
}
