plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.multimidiacivic.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.multimidiacivic.launcher"
        minSdk = 17
        targetSdk = 34
        versionCode = 2
        versionName = "0.2"
    }

    buildTypes {
        release {
            // Encolhe e otimiza o codigo na versao final
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { buildConfig = false }
    lint { abortOnError = false }
}

// Nenhuma dependencia extra: so o Android "puro" + Kotlin. Leve de proposito.
dependencies {}
