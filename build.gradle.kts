import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

group = "com.pixelgram.desktop"
version = "0.32.0"
val codename = "12-side cookie"

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
    implementation(libs.coroutines.swing)
    implementation(libs.material.color.utilities)
    implementation(libs.qrcodegen)
    implementation(libs.jna)
    implementation(libs.filekit.dialogs)
    implementation(libs.markdown)
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

val macSigningIdentity: String? = providers.environmentVariable("CODESIGN_IDENTITY").orNull?.takeIf { it.isNotBlank() }
    ?: providers.gradleProperty("pixelgram.macSigningIdentity").orNull?.takeIf { it.isNotBlank() }

// jpackage on macOS rejects a bundle version whose major component is zero.
val nativePackageVersion: String = run {
    val isMac = System.getProperty("os.name").lowercase().contains("mac")
    if (!isMac) return@run installerVersion
    val parts = installerVersion.split('.')
    if ((parts.firstOrNull()?.toIntOrNull() ?: 1) > 0) installerVersion else "1" + installerVersion.substringAfter('.', "")
        .let { if (it.isEmpty()) "" else ".$it" }
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
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Dmg)
            packageName = "pixelGram"
            packageVersion = nativePackageVersion
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
            macOS {
                bundleID = "com.pixelgram.desktop"
                dockName = "pixelGram"
                iconFile.set(project.file("packaging/icons/pixelgram.icns"))
                appCategory = "public.app-category.social-networking"
            }
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    System.getProperties()
        .filterKeys { it.toString().startsWith("pixelgram.") }
        .forEach { (key, value) -> systemProperty(key.toString(), value) }
}

// jpackage has no option for usage descriptions or URL types, so the macOS app
// bundle metadata is patched after imaging and signed ad hoc afterwards.
if (System.getProperty("os.name").lowercase().contains("mac")) {
    tasks.matching { it.name == "createDistributable" }.configureEach {
        inputs.property("pixelgram.macSigningIdentity", macSigningIdentity ?: "-")
        doLast {
            val app = layout.buildDirectory.dir("compose/binaries/main/app/pixelGram.app").get().asFile
            check(app.isDirectory) { "Missing macOS app image at ${app.absolutePath}" }
            val plist = File(app, "Contents/Info.plist")
            check(plist.isFile) { "Missing Info.plist at ${plist.absolutePath}" }

            fun plist(vararg arguments: String): Boolean {
                val command = mutableListOf("/usr/libexec/PlistBuddy")
                arguments.forEach { command += listOf("-c", it) }
                command += plist.absolutePath
                val process = ProcessBuilder(command).redirectErrorStream(true).start()
                val output = process.inputStream.bufferedReader().readText().trim()
                val ok = process.waitFor() == 0
                if (!ok && output.isNotEmpty()) logger.lifecycle("PlistBuddy: $output")
                return ok
            }

            fun setString(key: String, value: String) {
                plist("Delete :$key")
                check(plist("Add :$key string '$value'")) { "Could not set $key" }
            }

            setString("CFBundleDisplayName", "pixelGram")
            setString("CFBundleShortVersionString", project.version.toString())
            setString("LSMinimumSystemVersion", "11.0")
            setString("NSHumanReadableCopyright", "GPLv3, JettaXP")
            setString("NSMicrophoneUsageDescription", "pixelGram records voice messages and calls.")
            setString("NSCameraUsageDescription", "pixelGram takes photos and makes video calls.")
            setString("LSApplicationCategoryType", "public.app-category.social-networking")
            plist("Delete :NSHighResolutionCapable")
            check(plist("Add :NSHighResolutionCapable bool true")) { "Could not enable high resolution" }

            plist("Delete :CFBundleURLTypes")
            check(plist("Add :CFBundleURLTypes array")) { "Could not add CFBundleURLTypes" }
            check(plist("Add :CFBundleURLTypes:0 dict")) { "Could not add URL type" }
            check(plist("Add :CFBundleURLTypes:0:CFBundleURLName string 'com.pixelgram.desktop'")) { "Could not add URL name" }
            check(plist("Add :CFBundleURLTypes:0:CFBundleRole string 'Viewer'")) { "Could not add URL role" }
            check(plist("Add :CFBundleURLTypes:0:CFBundleURLSchemes array")) { "Could not add URL schemes" }
            check(plist("Add :CFBundleURLTypes:0:CFBundleURLSchemes:0 string 'tg'")) { "Could not add tg scheme" }

            val identity = macSigningIdentity ?: "-"
            val command = mutableListOf("codesign", "--force", "--deep", "--sign", identity)
            if (identity == "-") command += "--timestamp=none"
            command += app.absolutePath
            val sign = ProcessBuilder(command).redirectErrorStream(true).start()
            val signOutput = sign.inputStream.bufferedReader().readText().trim()
            check(sign.waitFor() == 0) { "Code signing failed: $signOutput" }
            logger.lifecycle(
                if (identity == "-") "Patched and ad-hoc signed ${app.name}"
                else "Patched and signed ${app.name} with $identity",
            )
        }
    }
}
