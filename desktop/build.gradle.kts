import com.diffplug.gradle.spotless.SpotlessExtension
import java.util.zip.ZipFile
import org.gradle.api.tasks.PathSensitivity
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

// CONTRACT-2：桌面端版本号同样来自根 gradle.properties，不再和 :app 各写一份
val ehviewerVersionName: String = providers.gradleProperty("ehviewer.versionName").orNull
    ?: throw GradleException("gradle.properties 缺少 ehviewer.versionName（版本号唯一来源）")

val rustDesktopDllFile = rootProject.file("app/src/main/rust/target-desk/x86_64-pc-windows-gnu/release/ehviewer_rust.dll")

// Copy 对不存在的源文件是空 no-op，因此这里不再用 onlyIf 闭包：
// Kotlin DSL 里的闭包会捕获脚本对象，configuration cache 无法序列化
// （报 "cannot serialize Gradle script object references"）。
val copyRustDll = tasks.register<Copy>("copyRustDll") {
    from(rustDesktopDllFile)
    into(layout.projectDirectory.dir("src/desktopMain/resources/native"))
}

// 把 ehviewer.versionName 生成为 desktopMain 的源文件，供 Updater.kt 以 BuildVersion.VERSION 引用。
// 用普通任务 + doLast 而不是自定义 Task 类：Kotlin DSL 脚本内声明的 class 会成为脚本类的
// 非静态内部类，Gradle 无法把它实例化成 Task 类型。
val generateBuildVersion = tasks.register("generateBuildVersion") {
    val outputDir = layout.buildDirectory.dir("generated/source/buildVersion/desktopMain/kotlin")
    val versionName = ehviewerVersionName
    inputs.property("versionName", versionName)
    outputs.dir(outputDir)
    doLast {
        val target = outputDir.get().asFile.resolve("com/ehviewer/desktop/BuildVersion.kt")
        target.parentFile.mkdirs()
        target.writeText(
            """
            |// 由 :desktop:generateBuildVersion 依据 gradle.properties 的 ehviewer.versionName 生成，勿手改
            |package com.ehviewer.desktop
            |
            |internal object BuildVersion {
            |    const val VERSION: String = "$versionName"
            |}
            |
            """.trimMargin(),
        )
    }
}

kotlin {
    jvm("desktop")

    jvmToolchain(libs.versions.java.get().toInt())

    sourceSets {
        getByName("desktopMain") {
            // 生成源码（BuildVersion.kt）：srcDir 传入 task provider 会同时建立 task 依赖
            kotlin.srcDir(generateBuildVersion)
            dependencies {
                implementation(compose.desktop.currentOs)
                // 共享层 launchUI/withUIContext 与 coil3 依赖 Dispatchers.Main；JVM 侧由 swing 模块经 ServiceLoader 提供
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.moko.resources.compose)
                implementation(libs.miuix.nav)
                implementation(projects.core.common)
                implementation(projects.core.data)
                implementation(projects.core.i18n)
                implementation(projects.core.shell)
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

// 同名 META-INF/services/<SPI> 在多份 jar 中各存一份，Jar 的 DuplicatesStrategy.EXCLUDE 只保留第一份，
// 其余提供者（Ktor/Coil/Coroutines 等）会被静默丢弃。这里先按 classpath 顺序把同名 SPI 文件逐行合并成一份，
// 再作为独立输入打进胖 jar，保证服务提供者不丢。
val desktopJarTask = tasks.named<Jar>("desktopJar")
val desktopRuntimeClasspath: FileCollection = configurations.getByName("desktopRuntimeClasspath")

val mergeServiceFiles = tasks.register("mergeServiceFiles") {
    val serviceClasspath = files(desktopRuntimeClasspath, desktopJarTask.flatMap { it.archiveFile })
    val outputDir = layout.buildDirectory.dir("generated/serviceFiles")
    inputs.files(serviceClasspath).withPathSensitivity(PathSensitivity.NONE)
    outputs.dir(outputDir)
    doLast {
        val providersByService = LinkedHashMap<String, LinkedHashSet<String>>()
        fun collect(serviceName: String, lines: List<String>) {
            val bucket = providersByService.getOrPut(serviceName) { LinkedHashSet() }
            lines.map { it.substringBefore('#').trim() }.filterTo(bucket) { it.isNotEmpty() }
        }
        fun collectEntry(entryName: String, readLines: () -> List<String>) {
            if (!entryName.startsWith("META-INF/services/")) return
            val serviceName = entryName.removePrefix("META-INF/services/")
            if (serviceName.isEmpty() || '/' in serviceName) return
            collect(serviceName, readLines())
        }
        serviceClasspath.files.forEach { entry ->
            when {
                entry.isDirectory -> entry.resolve("META-INF/services").listFiles()?.sortedBy { it.name }?.forEach { file ->
                    collectEntry("META-INF/services/" + file.name) { file.readLines() }
                }
                entry.isFile && entry.extension == "jar" -> ZipFile(entry).use { zip ->
                    zip.entries().asSequence().filter { !it.isDirectory }.forEach { zipEntry ->
                        collectEntry(zipEntry.name) {
                            zip.getInputStream(zipEntry).bufferedReader(Charsets.UTF_8).use { it.readLines() }
                        }
                    }
                }
            }
        }
        val target = outputDir.get().asFile
        target.deleteRecursively()
        providersByService.forEach { (serviceName, providers) ->
            target.resolve("META-INF/services/" + serviceName).apply { parentFile.mkdirs() }.writeText(
                providers.joinToString(separator = "\n", postfix = "\n"),
            )
        }
    }
}

val desktopFatJar = tasks.register<Jar>("desktopFatJar") {
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "com.ehviewer.desktop.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(zipTree(desktopJarTask.flatMap { it.archiveFile })) {
        exclude("META-INF/services/**")
    }
    from(desktopRuntimeClasspath.map { if (it.isDirectory) it else zipTree(it) }) {
        exclude(
            "META-INF/services/**",
            "META-INF/*.SF",
            "META-INF/*.DSA",
            "META-INF/*.RSA",
            "META-INF/versions/**/module-info.class",
            "module-info.class",
        )
    }
    from(mergeServiceFiles)
}

// Rust dll 由 copyRustDll 复制到 desktopMain 资源目录后才会被进程资源收集，这里显式建立依赖，
// 避免 :desktop:run / :desktop:desktopFatJar 拿到上一轮的旧 dll 或干脆缺文件。
tasks.matching { it.name == "desktopProcessResources" }.configureEach {
    dependsOn(copyRustDll)
}

// :desktop:run 时把命令行 -D 系统属性透传给应用 JVM（如 ehviewer.data.dir 重定向测试数据目录）。
// 只转发用户显式传入的 -D，不再遍历整个系统属性表：语义更准，也不会把 JVM 内部属性逐条读成配置缓存输入。
tasks.withType<JavaExec>().configureEach {
    gradle.startParameter.systemPropertiesArgs.forEach { (key, value) ->
        if (key.startsWith("ehviewer.")) systemProperty(key, value)
    }
}

compose.desktop {
    application {
        mainClass = "com.ehviewer.desktop.MainKt"
        jvmArgs("-Xmx512m")

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "EhViewer"
            packageVersion = ehviewerVersionName
        }
    }
}
