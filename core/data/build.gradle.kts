plugins {
    alias(libs.plugins.ehviewer.multiplatform.library)
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
            }
        }
        androidMain {
            dependencies {
                implementation(project.dependencies.platform(libs.okhttp.bom))
                implementation(libs.ktor.client.okhttp)
            }
        }
    }
}

dependencies {
    add("desktopMainImplementation", "androidx.sqlite:sqlite-bundled-jvm:2.7.1")
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}
