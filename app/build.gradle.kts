import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// 读取根目录下的.env文件
val envFile = rootProject.file(".env")
val envProperties = Properties()
if (envFile.exists()) {
    envFile.inputStream().use { envProperties.load(it) }
}

android {
    namespace = "com.example.idacardocr"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.idacardocr"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // 从.env文件读取API密钥并注入到BuildConfig
        buildConfigField("String", "TENCENT_SECRET_ID", "\"${envProperties.getProperty("TENCENT_SECRET_ID", "")}\"")
        buildConfigField("String", "TENCENT_SECRET_KEY", "\"${envProperties.getProperty("TENCENT_SECRET_KEY", "")}\"")
    }
    
    buildFeatures {
        buildConfig = true
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // OkHttp for network requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Biometric authentication
    implementation("androidx.biometric:biometric:1.1.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}