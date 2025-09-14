// app/build.gradle.kts
import java.util.Properties  // ⬅️ 추가

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

// ⬇️ local.properties / -P / 환경변수에서 값 읽기 헬퍼
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun prop(name: String): String? =
    localProps.getProperty(name)
        ?: (project.findProperty(name) as? String)
        ?: System.getenv(name)

android {
    namespace = "com.example.exitsw"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.exitsw"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Kakao (기존 유지)
        val kakaoKey = project.findProperty("KAKAO_APP_KEY") as? String
            ?: throw GradleException("local.properties에 KAKAO_APP_KEY가 정의되어 있지 않습니다.")
        manifestPlaceholders["KAKAO_APP_KEY"] = kakaoKey

        // ✅ Gemini 키/엔드포인트를 BuildConfig로 주입
        val geminiKey = prop("GEMINI_API_KEY")
            ?: throw GradleException("GEMINI_API_KEY가 없습니다. local.properties 또는 환경변수/Gradle -P 로 설정하세요.")
        val geminiEndpoint = prop("GEMINI_ENDPOINT")
            ?: "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
        buildConfigField("String", "GEMINI_ENDPOINT", "\"$geminiEndpoint\"")

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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    implementation("com.kakao.sdk:v2-user:2.21.4")
    implementation("com.kakao.sdk:v2-auth:2.21.4")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

}
