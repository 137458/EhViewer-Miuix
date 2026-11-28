plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.spotless)
}

android {
    namespace = "com.ehviewer.baselineprofile"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    flavorDimensions += listOf("api")
    productFlavors {
        create("default") { dimension = "api" }
        create("marshmallow") { dimension = "api" }
    }

    // :app 的 release 只编入 arm64-v8a，而入库的 libehviewer.so 也只有 arm64-v8a；
    // aosp-atd 没有 arm64 镜像，x86_64 模拟器装不上默认产物，因此托管设备仅在
    // -PbuildNative（x86_64 原生库随之编入）时启用，否则退回已连接的 arm64 真机。
    if (project.hasProperty("buildNative")) {
        testOptions.managedDevices.localDevices {
            create("pixel6Api35") {
                device = "Pixel 6"
                apiLevel = 35
                testedAbi = "x86_64"
                systemImageSource = "aosp-atd"
            }
        }
    }
}

kotlin {
    val javaVersion = libs.versions.java.get().toInt()
    jvmToolchain(javaVersion)
}

// 本模块此前没有任何格式门禁（其余模块经 ehviewer.* 约定插件接入 Spotless）
spotless {
    val ktlintVersion = libs.ktlint.get().version
    kotlin {
        target("src/**/*.kt")
        ktlint(ktlintVersion)
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint(ktlintVersion)
    }
}

baselineProfile {
    if (project.hasProperty("buildNative")) {
        managedDevices += "pixel6Api35"
        useConnectedDevices = false
    } else {
        // 默认只有 arm64-v8a 原生库，托管 x86_64 模拟器无法安装被测 APK
        useConnectedDevices = true
    }
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}

androidComponents {
    onVariants { v ->
        val artifactsLoader = v.artifacts.getBuiltArtifactsLoader()
        v.instrumentationRunnerArguments.put(
            "targetAppId",
            v.testedApks.map { artifactsLoader.load(it)!!.applicationId },
        )
    }
}
