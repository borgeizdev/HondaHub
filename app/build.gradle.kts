import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Chave da versao final: lida de assinatura.properties (fora do git).
val assinatura = Properties().apply {
    val f = rootProject.file("assinatura.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.multimidiacivic.launcher"
    compileSdk = 34

    signingConfigs {
        create("hondahub") {
            // Sem o arquivo, so a versao final falha (de proposito: assinar com outra
            // chave faria a atualizacao nao instalar por cima no carro).
            assinatura.getProperty("arquivo")?.let { storeFile = file(it) }
            storePassword = assinatura.getProperty("senha")
            keyAlias = assinatura.getProperty("apelido")
            keyPassword = assinatura.getProperty("senha")
        }
    }

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
            signingConfig = signingConfigs.getByName("hondahub")
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
