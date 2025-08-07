plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.majorware.echosms"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.majorware.echosms"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.mpandroidchart)
    implementation(platform(libs.firebase.bom)) // Le BOM
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.androidxcoresplashscreen)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}