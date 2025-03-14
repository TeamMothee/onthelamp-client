import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
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

    buildFeatures{
        dataBinding=true
        viewBinding=true
    }

    buildFeatures{
        dataBinding=true
        viewBinding=true
        mlModelBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Jetpack Compose integration
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Views/Fragments integration
    implementation("androidx.navigation:navigation-fragment:2.8.0")
    implementation("androidx.navigation:navigation-ui:2.8.0")

    // Feature module support for Fragments
    implementation("androidx.navigation:navigation-dynamic-features-fragment:2.8.0")

    // Testing Navigation
    androidTestImplementation("androidx.navigation:navigation-testing:2.8.0")

    implementation("androidx.camera:camera-core:1.1.0-beta01")
    implementation("androidx.camera:camera-camera2:1.1.0-beta01")
    implementation("androidx.camera:camera-lifecycle:1.1.0-beta01")
    implementation("androidx.camera:camera-video:1.1.0-beta01")

    implementation("androidx.camera:camera-view:1.1.0-beta01")
    implementation("androidx.camera:camera-extensions:1.1.0-beta01")

    implementation ("com.google.android.gms:play-services-location:21.0.1")

    implementation ("com.google.code.gson:gson:2.8.9")


//    implementation("androidx.navigation:navigation-safe-args-gradle-plugin:2.7.3")
//
//    implementation("androidx.camera:camera-core:1.5.0")
//    implementation("androidx.camera:camera-camera2:1.5.0")
//    implementation("androidx.camera:camera-lifecycle:1.5.0")
//    implementation("androidx.camera:camera-view:1.5.0")
//    implementation("androidx.camera:camera-extensions:1.5.0")

    // TMAP
    implementation(files("libs/tmap-sdk-1.5.aar"))
    implementation(files("libs/vsm-tmap-sdk-v2-android-1.6.60.aar"))

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")


    // Jetpack Compose integration
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // Views/Fragments integration
    implementation("androidx.navigation:navigation-fragment:2.8.0")
    implementation("androidx.navigation:navigation-ui:2.8.0")

    // Feature module support for Fragments
    implementation("androidx.navigation:navigation-dynamic-features-fragment:2.8.0")

    // Testing Navigation
    androidTestImplementation("androidx.navigation:navigation-testing:2.8.0")

    implementation("androidx.camera:camera-core:1.1.0-beta01")
    implementation("androidx.camera:camera-camera2:1.1.0-beta01")
    implementation("androidx.camera:camera-lifecycle:1.1.0-beta01")
    implementation("androidx.camera:camera-video:1.1.0-beta01")

    implementation("androidx.camera:camera-view:1.1.0-beta01")
    implementation("androidx.camera:camera-extensions:1.1.0-beta01")
//
//    implementation("androidx.camera:camera-core:1.5.0")
//    implementation("androidx.camera:camera-camera2:1.5.0")
//    implementation("androidx.camera:camera-lifecycle:1.5.0")
//    implementation("androidx.camera:camera-view:1.5.0")
//    implementation("androidx.camera:camera-extensions:1.5.0")
}