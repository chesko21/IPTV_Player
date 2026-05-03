import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.chesko.stream_pro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chesko.stream_pro"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "2.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            storeFile = file(storePath)
            storePassword = secrets.getProperty("RELEASE_STORE_PASSWORD") ?: ""
            keyAlias = secrets.getProperty("RELEASE_KEY_ALIAS") ?: ""
            keyPassword = secrets.getProperty("RELEASE_KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        debug {
            ndk {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
            }
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
    ndkVersion = "27.1.12297006"

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.converter.scalars)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.accompanist.permissions)
    implementation(libs.play.services.location)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.core)
    implementation(libs.logging.interceptor)
    implementation(libs.okhttp)
    implementation(libs.moshi.kotlin)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.material)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.dash)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.exoplayer.rtsp)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.cast)
    implementation(libs.androidx.mediarouter)
    implementation(libs.play.services.cast.framework)
    implementation(libs.libvlc.all)

    ksp(libs.androidx.room.compiler)
    ksp(libs.moshi.kotlin.codegen)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}