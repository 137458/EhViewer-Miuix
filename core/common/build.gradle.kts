plugins {
    alias(libs.plugins.ehviewer.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.io)
                api(libs.okio)
                api(libs.serialization.cbor)
                api(project.dependencies.platform(libs.arrow.stack))
                api(libs.bundles.arrow)
            }
        }
        androidMain {
            dependencies {
                api(libs.kotlinx.coroutines.android)
                api(libs.splitties.appctx)
                api(libs.logcat)
                implementation(libs.androidx.core)
            }
        }
    }
}
