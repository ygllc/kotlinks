
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(ktorLibs.plugins.ktor)
}

group = "com.yg"
version = "1.0.0"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.logback.classic)
    implementation(libs.akuleshov7.ktoml.core)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
