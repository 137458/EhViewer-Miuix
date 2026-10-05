import com.diffplug.gradle.spotless.SpotlessExtension
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
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
                // 共享层 launchUI/withUIContext 与 coil3 依赖 Dispatchers.Main；JVM 侧由 swing 模块经 ServiceLoader 提供
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.moko.resources.compose)
                implementation(projects.core.common)
                implementation(projects.core.data)
                implementation(projects.core.i18n)
                implementation(projects.core.ui)
                implementation(libs.serialization.json)
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

// 可执行胖 jar：单文件分发（系统 JRE 即可运行，绕开 jpackage 依赖）。
// 解包 desktop-desktop.jar + runtime classpath；Rust dll 已随资源在薄 jar 内。
val desktopJarTask = tasks.named<Jar>("desktopJar")
val desktopRuntimeClasspath: FileCollection = configurations.getByName("desktopRuntimeClasspath")

val desktopFatJar = tasks.register<Jar>("desktopFatJar") {
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "com.ehviewer.desktop.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(desktopJarTask)
    from(zipTree(desktopJarTask.map { it.archiveFile }))
    from(desktopRuntimeClasspath.map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/**/module-info.class", "module-info.class")
    }
}

// :desktop:run 时把命令行 -D 系统属性透传给应用 JVM（如 ehviewer.data.dir 重定向测试数据目录）
tasks.withType<JavaExec>().configureEach {
    System.getProperties().forEach { (k, v) ->
        if (k.toString().startsWith("ehviewer.")) systemProperty(k.toString(), v.toString())
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
