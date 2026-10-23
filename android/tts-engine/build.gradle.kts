plugins {
    id("com.android.application") version "9.1.1"
    id("org.jetbrains.kotlin.android") version "2.2.20"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"
}

// The engine speaks through the same Kotlin wrapper apps copy, android/PhononTTS.kt, so it is
// compiled from there rather than kept as a second copy.
abstract class ShareWrapper : DefaultTask() {
    @get:InputFile abstract val source: RegularFileProperty
    @get:OutputDirectory abstract val output: DirectoryProperty

    @TaskAction
    fun copy() {
        source.get().asFile.copyTo(output.file("PhononTTS.kt").get().asFile, overwrite = true)
    }
}
val shareWrapper = tasks.register<ShareWrapper>("shareWrapper") { source.set(file("../PhononTTS.kt")) }
androidComponents.onVariants { it.sources.java?.addGeneratedSourceDirectory(shareWrapper, ShareWrapper::output) }

android {
    namespace = "ai.gradium.phonon.tts"
    compileSdk = 37
    defaultConfig {
        applicationId = "ai.gradium.phonon.tts"
        minSdk = 24
        targetSdk = 36
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        versionCode = 8
        versionName = "0.4.1-android.7"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // What android/build.sh writes.
    sourceSets["main"].jniLibs.srcDir("../jniLibs")
    buildTypes {
        release {
            isMinifyEnabled = true
            // So a release build installs for testing. Sign a published build with your own key.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    testImplementation("junit:junit:4.13.2")
    implementation("net.java.dev.jna:jna:5.17.0@aar")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

// Keep focused-test temporary files in the caller's scratch directory.
tasks.withType<Test>().configureEach {
    System.getenv("TMPDIR")?.let { systemProperty("java.io.tmpdir", it) }
}
