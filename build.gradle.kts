import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

group = "com.pixelgram.desktop"
version = "0.17.0"
val codename = "Update"

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

val bytedecoPlatform: String = run {
    val os = System.getProperty("os.name").lowercase()
    when {
        os.contains("win") -> "windows-x86_64"
        os.contains("mac") -> "macosx-arm64"
        else -> "linux-x86_64"
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.coroutines.swing)
    implementation(libs.material.color.utilities)
    implementation(libs.qrcodegen)
    implementation(libs.jna)
    implementation(libs.filekit.dialogs)
    implementation(libs.concentus)
    implementation(libs.jlayer)
    implementation(libs.javacv) { isTransitive = false }
    implementation(libs.javacpp)
    implementation(libs.ffmpeg)
    implementation(variantOf(libs.javacpp) { classifier(bytedecoPlatform) })
    implementation(variantOf(libs.ffmpeg) { classifier(bytedecoPlatform) })

    implementation(platform(libs.tdlight.bom))
    implementation(libs.tdlight.java)
    implementation(group = "it.tdlight", name = "tdlight-natives", classifier = tdlightNatives)
}

val generateBuildConfig by tasks.registering {
    val apiId = providers.environmentVariable("TELEGRAM_API_ID").filter { it.isNotBlank() }
        .orElse(providers.gradleProperty("pixelgram.apiId"))
        .orElse("")
    val apiHash = providers.environmentVariable("TELEGRAM_API_HASH").filter { it.isNotBlank() }
        .orElse(providers.gradleProperty("pixelgram.apiHash"))
        .orElse("")
    val appVersion = project.version.toString()
    val appCodename = codename
    val commit = providers.environmentVariable("GITHUB_SHA").filter { it.isNotBlank() }.orElse("dev")
    val outputDir = layout.buildDirectory.dir("generated/buildconfig")
    inputs.property("apiId", apiId)
    inputs.property("apiHash", apiHash)
    inputs.property("version", appVersion)
    inputs.property("codename", appCodename)
    inputs.property("commit", commit)
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
            |    const val CODENAME = "$appCodename"
            |    const val COMMIT = "${commit.get()}"
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

val installerVersion: String = run {
    val build = providers.environmentVariable("GITHUB_RUN_NUMBER").orNull?.toIntOrNull()
    val base = project.version.toString()
    if (build == null) base else base.substringBeforeLast('.') + "." + build
}

compose.desktop {
    application {
        mainClass = "com.pixelgram.desktop.MainKt"
        jvmArgs += listOf(
            "-Xmx1g",
            "-XX:MinHeapFreeRatio=10",
            "-XX:MaxHeapFreeRatio=30",
            "-XX:G1PeriodicGCInterval=30000",
        )

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi, TargetFormat.Exe)
            packageName = "pixelGram"
            packageVersion = installerVersion
            description = "pixelGram - Telegram client with Material 3"
            vendor = "jettax"
            licenseFile.set(project.file("LICENSE"))
            appResourcesRootDir.set(project.layout.projectDirectory.dir("appResources"))
            modules("java.sql", "jdk.unsupported", "java.naming", "jdk.security.auth")

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

tasks.withType<JavaExec>().configureEach {
    System.getProperties()
        .filterKeys { it.toString().startsWith("pixelgram.") }
        .forEach { (key, value) -> systemProperty(key.toString(), value) }
}
