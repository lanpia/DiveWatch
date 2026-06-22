plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.dive.mobile"
    compileSdk = 36

    defaultConfig {
        // ⚠ 워치 앱과 동일해야 데이터 레이어 통신이 됩니다.
        applicationId = "com.example.dive"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.serialization.json)
    implementation("androidx.core:core-ktx:1.10.1")
    // Google 로그인 + OAuth 토큰(Drive 업로드용). 무거운 Drive Java 클라이언트 대신
    // REST 멀티파트 업로드를 직접 수행하므로 이 의존성만 필요.
    implementation("com.google.android.gms:play-services-auth:20.7.0")
}
