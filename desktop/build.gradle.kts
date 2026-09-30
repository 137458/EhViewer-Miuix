import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm("desktop")

    jvmToolchain(libs.versions.java.get().toInt())

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.moko.resources.compose)
                implementation(projects.core.common)
                implementation(projects.core.data)
                implementation(projects.core.i18n)
                implementation(projects.core.ui)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.ehviewer.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "EhViewer"
            packageVersion = "1.0.0"
        }
    }
}
