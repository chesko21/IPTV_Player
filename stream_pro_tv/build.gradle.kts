plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.chesko.stream_pro_tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chesko.stream_pro_tv"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../app/my-release-key.jks")
            storePassword = "chesko123"
            keyAlias = "stream-pro-key"
            keyPassword = "chesko123"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
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
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.leanback)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.foundation)
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
    implementation(libs.libvlc.all)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}