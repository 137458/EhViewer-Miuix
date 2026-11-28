import com.ehviewer.CheckNativeLibsTask
import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import com.mikepenz.aboutlibraries.plugin.DuplicateRule
import java.util.regex.Pattern

// 只读一次 startParameter：get() 会在每次访问时重新求值，多个配置点分散读取既慢又不利于配置缓存输入追踪。
val isRelease: Boolean = gradle.startParameter.taskNames.any { it.contains("Release") }

plugins {
    alias(libs.plugins.ehviewer.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.aboutlibrariesAndroid)
    alias(libs.plugins.baselineprofile)
}

// 发布/内置原生库的 ABI 白名单：仓库只入库 arm64-v8a 的 libehviewer.so（见 .gitignore 与 AGENTS.md）
val supportedAbis = arrayOf("arm64-v8a")

// x86_64 只有带 -PbuildNative 时才会产出原生库，否则 split 出来的 APK 没有 libehviewer.so、装机即崩，
// 因此默认不纳入。发布产物只保留单 ABI split，不额外产出内容完全相同的 universal APK。
val abiWithNative: Array<String> = if (hasProperty("buildNative")) supportedAbis + "x86_64" else supportedAbis

// CONTRACT-2：版本号唯一来源为根 gradle.properties 的 ehviewer.versionName / ehviewer.versionCode
val ehviewerVersionName: String = providers.gradleProperty("ehviewer.versionName").orNull
    ?: throw GradleException("gradle.properties 缺少 ehviewer.versionName（版本号唯一来源）")
val ehviewerVersionCode: Int = providers.gradleProperty("ehviewer.versionCode").orNull?.toIntOrNull()
    ?: throw GradleException("gradle.properties 缺少合法的 ehviewer.versionCode（版本号唯一来源）")

// 构建元信息取自 git。非 git 工作副本（源码归档）、浅克隆或缺少 git 可执行文件时降级为默认值，
// 不让"环境里没有 git"变成配置阶段的构建失败。
fun gitValue(fallback: String, vararg arguments: String): String = runCatching {
    providers.exec {
        commandLine(listOf("git") + arguments)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
}.getOrNull()?.takeIf { it.isNotEmpty() } ?: fallback

android {
    splits {
        abi {
            isEnable = true
            reset()
            if (isRelease) {
                include(*supportedAbis)
                isUniversalApk = false
            } else {
                include(*abiWithNative)
            }
        }
    }

    // CONTRACT-5：签名材料只从 Gradle 属性 / 同名环境变量读取，四项都没提供时 release 不签名
    // （本地免密钥可编译）。绝不回退到入库密钥库：提交进仓库的密钥应按泄漏处理。
    val releaseSigningKeys = listOf("RELEASE_STORE_FILE", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD")
    val releaseSigningValues = releaseSigningKeys.associateWith { key ->
        providers.gradleProperty(key).orElse(providers.environmentVariable(key)).orNull
    }
    val releaseSigningMissing = releaseSigningKeys.filter { releaseSigningValues[it].isNullOrBlank() }
    if (releaseSigningMissing.isNotEmpty() && releaseSigningMissing.size < releaseSigningKeys.size) {
        throw GradleException(
            "release 签名配置不完整，缺少：${releaseSigningMissing.joinToString()}。\n" +
                "  四项需同时提供（-PRELEASE_* Gradle 属性或同名环境变量）；四项都未提供时按不签名处理。",
        )
    }
    val signConfig = if (releaseSigningMissing.isEmpty()) {
        signingConfigs.create("release") {
            storeFile = File(releaseSigningValues.getValue("RELEASE_STORE_FILE")!!)
            storePassword = releaseSigningValues.getValue("RELEASE_STORE_PASSWORD")
            keyAlias = releaseSigningValues.getValue("RELEASE_KEY_ALIAS")
            keyPassword = releaseSigningValues.getValue("RELEASE_KEY_PASSWORD")
            enableV3Signing = true
            enableV4Signing = true
        }
    } else {
        logger.lifecycle("未提供 RELEASE_STORE_FILE/RELEASE_STORE_PASSWORD/RELEASE_KEY_ALIAS/RELEASE_KEY_PASSWORD，release 构建产出未签名 APK。")
        null
    }

    val commitSha = gitValue("unknown", "rev-parse", "--short=7", "HEAD")
    val commitTime = gitValue("0", "log", "-1", "--format=%ct")
    val repoName = gitValue("137458/EhViewer-Miuix", "remote", "get-url", "origin")
        .removePrefix("https://github.com/").removePrefix("git@github.com:").removeSuffix(".git")

    val snapshot = !hasProperty("release")

    defaultConfig {
        applicationId = "moe.tarsin.ehviewer"
        versionCode = ehviewerVersionCode
        versionName = if (snapshot) "$ehviewerVersionName-SNAPSHOT" else ehviewerVersionName
        buildConfigField("boolean", "SNAPSHOT", "$snapshot")
        buildConfigField("String", "RAW_VERSION_NAME", "\"$versionName\"")
        buildConfigField("String", "COMMIT_SHA", "\"$commitSha\"")
        buildConfigField("long", "COMMIT_TIME", commitTime)
        buildConfigField("String", "REPO_NAME", "\"$repoName\"")
        // 不要在 defaultConfig 再设 ndk.abiFilters：splits.abi 已按 ABI 拆分，
        // 两者同时命中同一 ABI 时 AGP 直接报 "Conflicting configuration"。
        ndk {
            debugSymbolLevel = "FULL"
        }
    }

    flavorDimensions += "api"

    productFlavors {
        create("default") {
            minSdk = 26
            applicationIdSuffix = ".miuix"
            versionNameSuffix = "-miuix"
        }
        create("marshmallow") {
            applicationIdSuffix = ".m"
            versionNameSuffix = "-M"
        }
    }

    if (project.hasProperty("buildNative")) {
        externalNativeBuild {
            cmake {
                path = File("src/main/cpp/CMakeLists.txt")
            }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        dex {
            useLegacyPackaging = false
        }
        jniLibs {
            excludes += "**/libdatastore_shared_counter.so" // DataStore multi-process
        }
        resources {
            // Required by Layout Inspector
            pickFirsts += "/META-INF/androidx.compose.ui_ui.version"

            excludes += listOf(
                "/META-INF/**",
                "/kotlin/**",
                "**.txt",
                "**.bin",
            )
        }
    }

    androidResources {
        ignoreAssetsPatterns += listOf(
            "!PublicSuffixDatabase.list", // OkHttp
        )
        generateLocaleConfig = true
        localeFilters += listOf(
            "zh",
            "zh-rCN",
            "zh-rHK",
            "zh-rTW",
            "es",
            "ja",
            "ko",
            "fr",
            "de",
            "th",
            "tr",
            "nb-rNO",
        )
    }

    dependenciesInfo.includeInApk = false

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            signingConfig = signConfig
        }
        debug {
            applicationIdSuffix = ".debug"
        }
        create("benchmarkRelease") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
            applicationIdSuffix = ".benchmark"
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    namespace = "com.hippo.ehviewer"
}

baselineProfile {
    mergeIntoMain = true
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.i18n)
    implementation(projects.core.shell)
    implementation(projects.core.ui)

    // https://developer.android.com/jetpack/androidx/releases/activity
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.webkit)

    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.compiler)

    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.androidx.datastore)

    // https://developer.android.com/jetpack/androidx/releases/lifecycle
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.compose)

    // https://developer.android.com/jetpack/androidx/releases/paging
    implementation(libs.androidx.paging.compose)

    // https://developer.android.com/jetpack/androidx/releases/room
    implementation(libs.androidx.room.paging)

    implementation(libs.androidx.work.runtime)
    implementation(libs.material.motion.core)
    implementation(libs.material.kolor)

    implementation(libs.bundles.splitties)

    // https://square.github.io/okhttp/changelogs/changelog/
    implementation(platform(libs.okhttp.bom))

    implementation(libs.logcat)

    implementation(libs.diff)

    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.accompanist.drawable.painter)

    implementation(libs.reorderable)

    implementation(platform(libs.arrow.stack))
    implementation(libs.bundles.arrow)

    // https://coil-kt.github.io/coil/changelog/
    implementation(platform(libs.coil.bom))
    implementation(libs.bundles.coil)

    implementation(libs.telephoto.zoomable)

    implementation(libs.ktor.client.okhttp)

    implementation(libs.bundles.kotlinx.serialization)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)

    coreLibraryDesugaring(libs.desugar)

    implementation(libs.androidx.profileinstaller)
    "baselineProfile"(project(":benchmark"))

    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlin.test.junit)
}

kotlin {
    compilerOptions {
        optIn.addAll(
            "coil3.annotation.ExperimentalCoilApi",
            "androidx.paging.ExperimentalPagingApi",
            "me.saket.telephoto.ExperimentalTelephotoApi",
        )
    }
}

ksp {
    arg("compose-destinations.codeGenPackageName", "com.hippo.ehviewer.ui")
}

aboutLibraries {
    collect {
        includePlatform = false
    }
    library {
        exclusionPatterns.add(Pattern.compile("org\\.jetbrains\\.(?:compose|androidx)\\..*"))
        duplicationMode = DuplicateMode.MERGE
        duplicationRule = DuplicateRule.GROUP
    }
}

// H-39：原生库校验固化为 typed task，类型定义在 build-logic/convention 的
// com.ehviewer.CheckNativeLibsTask（Kotlin DSL 脚本内的 class 是非静态内部类，无法作为 Task 类型实例化）。
if (!project.hasProperty("buildNative") && !project.hasProperty("skipNativeCheck")) {
    val checkNativeLibs = tasks.register<CheckNativeLibsTask>("checkNativeLibs") {
        nativeLibs.from(supportedAbis.map { abi -> projectDir.resolve("src/main/jniLibs/$abi/libehviewer.so") })
        requiredJniSymbols.set(
            listOf(
                "JNI_OnLoad",
                // app/src/main/kotlin/com/hippo/ehviewer/jni/Archive.kt
                "Java_com_hippo_ehviewer_jni_ArchiveKt_openArchive",
                "Java_com_hippo_ehviewer_jni_ArchiveKt_closeArchive",
                "Java_com_hippo_ehviewer_jni_ArchiveKt_archiveWriteOpen",
                "Java_com_hippo_ehviewer_jni_ArchiveKt_archiveWriteEntry",
                "Java_com_hippo_ehviewer_jni_ArchiveKt_archiveWriteClose",
                // app/src/main/kotlin/com/hippo/ehviewer/jni/GifUtils.kt
                "Java_com_hippo_ehviewer_jni_GifUtilsKt_rewriteGifSource",
                // app/src/main/kotlin/com/hippo/ehviewer/image/Image.kt
                "Java_com_hippo_ehviewer_image_ImageKt_detectBorder",
                "Java_com_hippo_ehviewer_image_ImageKt_copyBitmapToAHB",
                // app/src/main/kotlin/com/hippo/ehviewer/client/parser/GalleryListParser.kt
                "Java_com_hippo_ehviewer_client_parser_GalleryListParserKt_parseGalleryInfoList",
            ),
        )
    }
    // 只在打包 APK/JAR 时校验：缺库或与 Kotlin 声明脱钩的 .so 不允许进产物，
    // 但编译、单测等开发流程不再被陈旧的入库 .so 阻塞。
    // 注意排除 AGP 的 package*Resources / package*Assets（资源打包，属于编译链路）。
    tasks.matching { task ->
        val name = task.name
        name.startsWith("assemble") || name.startsWith("bundle") ||
            (name.startsWith("package") && !name.contains("Resources") && !name.contains("Assets"))
    }.configureEach {
        dependsOn(checkNativeLibs)
    }
}
