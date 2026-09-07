import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "com.soheib-ta"
version = "1.0.1"

kotlin {
    jvm()

    androidLibrary {
        namespace = "com.soheibta.karbon"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilations.configureEach {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_11)
            }
        }
    }

    iosArm64()
    iosSimulatorArm64()

    js {
        browser()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

//    signAllPublications()

    coordinates("com.soheib-ta", "karbon", version.toString())

    pom {
        name = "Karbon"
        description = "Karbon charts library for Kotlin Multiplatform."
        inceptionYear = "2026"
        url = "https://github.com/soheib-ta/karbon/"
        licenses {
            license {
                name = "XXX"
                url = "YYY"
                distribution = "ZZZ"
            }
        }
        developers {
            developer {
                id = "soheib-ta"
                name = "Soheib Taleb"
                url = "https://github.com/soheib-ta/"
            }
        }
        scm {
            url = "https://github.com/soheib-ta/karbon/"
            connection = "scm:git:git://github.com/soheib-ta/karbon.git"
            developerConnection = "scm:git:ssh://git@github.com/soheib-ta/karbon.git"
        }
    }
}
