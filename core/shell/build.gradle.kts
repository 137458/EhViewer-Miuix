plugins {
    alias(libs.plugins.ehviewer.multiplatform.library.compose)
}

kotlin {
    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                api(projects.core.common)
                api(projects.core.data)
                api(projects.core.i18n)
                api(projects.core.ui)
                api(libs.androidx.paging.compose)
                implementation(libs.compose.ui.backhandler)
                implementation(libs.compose.ui.tooling.preview)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        androidMain {
            dependencies {
                api(project.dependencies.platform(libs.compose.bom))
                api(libs.compose.foundation)
                implementation(libs.androidx.activity.compose)
            }
        }
    }
}
