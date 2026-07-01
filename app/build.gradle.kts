import org.gradle.kotlin.dsl.libs
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {

    signingConfigs {
        create("release") {
            storeFile = file("simpleCalculatorAndroid.jks")
            storePassword = "Ritesh@2001"
            keyAlias = "simpleCalculatorAndroid"
            keyPassword = "Ritesh@2001"
        }
    }

    namespace = "com.riteshkatre.simplecalculator"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.riteshkatre.simplecalculator"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            resValue("string", "admob_app_id", "ca-app-pub-3940256099942544~3347511713")
            resValue("string", "admob_banner_unit_id", "ca-app-pub-3940256099942544/6300978111")
            resValue("string", "admob_interstitial_unit_id", "ca-app-pub-3940256099942544/1033173712")
            resValue("string", "admob_rewarded_unit_id", "ca-app-pub-3940256099942544/5224354917")
            resValue("string", "admob_native_unit_id", "ca-app-pub-3940256099942544/2247696110")
            resValue("string", "admob_app_open_unit_id", "ca-app-pub-3940256099942544/3419835294")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.google.android.gms:play-services-ads:25.4.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
