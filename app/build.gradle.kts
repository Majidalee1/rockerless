import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "app.volumebubble"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.bytecrest.rockerless"
        minSdk = 30
        targetSdk = 36
        versionCode = 5
        versionName = "1.2.2"
    }

    signingConfigs {
        create("release") {
            storeFile = keystoreProps["storeFile"]?.let { rootProject.file(it as String) }
            storePassword = keystoreProps["storePassword"] as String?
            keyAlias = keystoreProps["keyAlias"] as String?
            keyPassword = keystoreProps["keyPassword"] as String?
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystorePropsFile.exists()) signingConfigs.getByName("release") else null
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
