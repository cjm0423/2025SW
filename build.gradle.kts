import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.JavaExec

plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.20"
    kotlin("plugin.spring") version "1.9.20"
    application
}

application {
    // 기본 run 태스크에 사용할 mainClass
    mainClass.set("com.youth.policy.GeminiApplicationKt")
}

group = "com.youth.policy"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.3")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.moshi:moshi:1.14.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")
    implementation("com.google.guava:guava:32.1.2-jre")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

// UTF-8 인코딩 보장
tasks.withType<JavaExec> {
    systemProperty("file.encoding", "UTF-8")
}

// 전용 실행 태스크: Gemini 콘솔 앱
tasks.register<JavaExec>("runGeminiApp") {
    group = "application"
    description = "콘솔용 GeminiApplication 실행"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.youth.policy.GeminiApplicationKt")
    // JVM 인코딩 강제 설정 (한글 깨짐 방지)
    jvmArgs = listOf("-Dfile.encoding=UTF-8")
    standardInput = System.`in`
}

// 전용 실행 태스크: API Gateway 앱
tasks.register<JavaExec>("runGatewayApp") {
    group = "application"
    description = "Spring Boot ApiGatewayApplication 실행"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.youth.policy.ApiGatewayApplicationKt")
    // JVM 인코딩 강제 설정 (한글 깨짐 방지)
    jvmArgs = listOf("-Dfile.encoding=UTF-8")
    standardInput = System.`in`
}
