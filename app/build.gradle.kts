plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.dive"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.dive"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
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

    kotlinOptions {
        jvmTarget = "11"
    }

    useLibrary("wear-sdk")

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))

    implementation(libs.play.services.wearable)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.tiles)
    implementation(libs.tiles.material)
    implementation(libs.tiles.tooling.preview)
    implementation(libs.horologist.compose.tools)
    implementation(libs.horologist.tiles)
    implementation(libs.watchface.complications.data.source.ktx)

    // ✅ AppCompat과 Core에서 support-v4 명시적으로 제외
    implementation("androidx.appcompat:appcompat:1.6.1") {
        exclude(group = "com.android.support", module = "support-v4")
    }
    implementation("androidx.core:core-ktx:1.10.1") {
        exclude(group = "com.android.support", module = "support-v4")
    }

    // ✅ graphview 라이브러리에서 support-v4 제외
    implementation("com.jjoe64:graphview:4.2.2") {
        exclude(group = "com.android.support", module = "support-v4")
    }

    // ✅ wear 라이브러리에서도 support-v4 제외
    implementation("androidx.wear:wear:1.2.0") {
        exclude(group = "com.android.support", module = "support-v4")
    }

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.tiles.tooling)
    implementation("androidx.compose.material3:material3:1.2.1") // 최신 버전 확인 권장
    implementation(platform("androidx.compose:compose-bom:2024.01.00"))
    implementation("androidx.compose.material3:material3")


}
