plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("maven-publish")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                groupId = "com.github.t8rin"
                artifactId = "image-text-reader"
                version = libs.versions.libVersion.get()
                from(components["release"])
            }
        }
    }
}

android {
    namespace = "com.t8rin.imagetextreader"

    compileSdk = 34
    defaultConfig.minSdk = 21

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.tesseract)
    implementation(libs.hilt)
    kapt(libs.dagger.hilt.compiler)
}