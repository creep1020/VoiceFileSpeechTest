
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.voicefilespeechtest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.voicefilespeechtest"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("com.google.mlkit:genai-speech-recognition:1.0.0-alpha2")
}

