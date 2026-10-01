plugins {
    alias(libs.plugins.ehviewer.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                api(libs.androidx.datastore)
                api(libs.androidx.room.paging)
                implementation(libs.ktor.client.core)
                implementation(libs.serialization.json)
            }
        }
        androidMain {
            dependencies {
                implementation(project.dependencies.platform(libs.okhttp.bom))
                implementation(libs.ktor.client.okhttp)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.serialization.cbor)
            }
        }
    }
}

dependencies {
    add("desktopMainImplementation", "androidx.sqlite:sqlite-bundled-jvm:2.7.1")
    add("desktopMainImplementation", libs.ktor.client.okhttp)
    // DPAPI 绑定：桌面 Cookie 落盘加密（锁 5.6.0 与本地缓存一致）
    add("desktopMainImplementation", "net.java.dev.jna:jna:5.6.0")
    add("desktopMainImplementation", "net.java.dev.jna:jna-platform:5.6.0")
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
