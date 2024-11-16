import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.onthelamp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.onthelamp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // BuildConfig 활성화
    buildFeatures {
        buildConfig = true
    }

    // local.properties에서 API 키를 읽어서 BuildConfig에 추가
    val localProperties = rootProject.file("local.properties")
    if (localProperties.exists()) {
        val properties = Properties().apply {
            load(localProperties.inputStream())
        }
        defaultConfig {
            buildConfigField(
                "String",
                "TMAP_API_KEY",
                "\"${properties["TMAP_API_KEY"]}\""
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(files("libs/tmap-sdk-1.5.aar"))
    implementation(files("libs/vsm-tmap-sdk-v2-android-1.6.60.aar"))
}