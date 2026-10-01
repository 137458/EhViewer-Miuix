import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.spotless)
}

configure<SpotlessExtension> {
    val ktlintVersion = libs.ktlint.get().version
    kotlin {
        // https://github.com/diffplug/spotless/issues/111
        target("src/**/*.kt")
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        ktlint(ktlintVersion)
    }
}

val rustDesktopDllFile = rootProject.file("app/src/main/rust/target-desk/x86_64-pc-windows-gnu/release/ehviewer_rust.dll")

val copyRustDll = tasks.register<Copy>("copyRustDll") {
    from(rustDesktopDllFile)
    into(layout.projectDirectory.dir("src/desktopMain/resources/native"))
    onlyIf { rustDesktopDllFile.exists() }
}

kotlin {
    jvm("desktop")

    jvmToolchain(libs.versions.java.get().toInt())

    sourceSets {
        getByName("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.moko.resources.compose)
                implementation(projects.core.common)
                implementation(projects.core.data)
                implementation(projects.core.i18n)
                implementation(projects.core.ui)
                implementation(project.dependencies.platform(libs.coil.bom))
                implementation(libs.coil.compose)
                implementation(libs.coil.network)
            }
        }
        getByName("desktopTest") {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.ehviewer.desktop.MainKt"
        jvmArgs("-Xmx512m")

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "EhViewer"
            packageVersion = "1.0.0"
        }
    }
}
