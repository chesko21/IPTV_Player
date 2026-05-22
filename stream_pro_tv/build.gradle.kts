import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.chesko.stream_pro_tv"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.chesko.stream_pro_tv"
        minSdk = 23
        targetSdk = 35
        versionCode = 2
        versionName = "2.1.2"

        ndk {
            val isRelease = project.gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }

            if (isRelease) {
                abiFilters += setOf("armeabi-v7a", "arm64-v8a", "x86_64")
            } else {
                abiFilters += setOf("armeabi-v7a", "arm64-v8a", "x86_64", "x86")
            }
        }
    }

    signingConfigs {
        val secretsFile = rootProject.file("secrets.properties")
        val secrets = Properties()
        if (secretsFile.exists()) {
            val input = secretsFile.inputStream()
            secrets.load(input)
            input.close()
        }

        create("release") {
            val storePath = secrets.getProperty("RELEASE_STORE_FILE") ?: "my-release-key.jks"

            storeFile = if (storePath.contains("app/")) {
                rootProject.file(storePath)
            } else {
                rootProject.file("app/$storePath")
            }
            
            storePassword = secrets.getProperty("RELEASE_STORE_PASSWORD") ?: ""
            keyAlias = secrets.getProperty("RELEASE_KEY_ALIAS") ?: ""
            keyPassword = secrets.getProperty("RELEASE_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.leanback)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.accompanist.permissions)

    implementation(libs.glide)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.rtsp)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.session)

    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)

    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.livedata)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)

    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.coil.compose)
    implementation(libs.material)
    implementation(libs.play.services.cast.framework)
    implementation(libs.play.services.ads)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}