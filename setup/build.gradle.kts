plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

group = "com.pixelgram.setup"
version = rootProject.version

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.material3)
    implementation(libs.coroutines.swing)
    implementation(libs.jna)
}

val writeSetupVersion by tasks.registering {
    val out = layout.buildDirectory.dir("generated/setup-resources")
    val text = rootProject.version.toString()
    inputs.property("version", text)
    outputs.dir(out)
    doLast { out.get().file("setup-version.txt").asFile.apply { parentFile.mkdirs() }.writeText(text) }
}

sourceSets.main {
    resources.srcDir(writeSetupVersion)
}

compose.desktop {
    application {
        mainClass = "com.pixelgram.setup.MainKt"
        nativeDistributions {
            packageName = "pixelGram Setup"
            packageVersion = rootProject.version.toString().substringBefore('-')
            vendor = "jettax"
            modules("jdk.unsupported")
            windows {
                iconFile.set(rootProject.file("packaging/icons/pixelgram.ico"))
            }
        }
    }
}
