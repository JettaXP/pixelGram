rootProject.name = "pixelgram"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        // TDLight: prebuilt TDLib with Java bindings.
        maven("https://mvn.mchv.eu/repository/mchv/")
    }
}
