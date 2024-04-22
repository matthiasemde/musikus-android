@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import java.util.Properties

val properties = Properties()
file("$rootDir/build.properties").inputStream().use { properties.load(it) }
val importedVersionCode = properties["versionCode"] as String
val importedVersionName = properties["versionName"] as String
val commitHash = properties["commitHash"] as String

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp")
    id("de.mannodermaus.android-junit5")
    id("com.jaredsburrows.license")
    id("androidx.room")
}

android {
    val javaVersion = JavaVersion.VERSION_17

    namespace = "app.musikus"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.musikus"
        minSdk = 24
        targetSdk = compileSdk
        versionCode = importedVersionCode.toInt()
        versionName = importedVersionName

        archivesName = "$applicationId-v$versionName"

        testInstrumentationRunner = "app.musikus.HiltTestRunner"

        buildConfigField("String", "COMMIT_HASH", "\"$commitHash\"")
    }

    signingConfigs {
        try {
            create("release") {
                storeFile = file(System.getenv("SIGNING_KEY_STORE_PATH"))
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        } catch (_: Exception) {
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release").takeIf {
                it.isSigningReady
            } ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    // needed for mockk
    testOptions {
        packaging {
            jniLibs { useLegacyPackaging = true }
        }
    }

    sourceSets {
        // Adds exported schema location as test app assets.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    kotlinOptions {
        jvmTarget = javaVersion.toString()
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

tasks.withType<Test> {
    useJUnitPlatform()
    reports {
        html.required = true
    }
}

dependencies {
    val bomVersion = "2024.03.00"

    val lifecycleVersion = "2.7.0"
    val roomVersion = "2.6.1"
    val navVersion = "2.7.7"
    val daggerHiltVersion = "2.49"
    val kotlinCoroutineVersion = "1.7.3"
    val mockkVersion = "1.13.9"
    val media3Version = "1.3.0"
    val activityVersion = "1.8.2"

    implementation("androidx.activity:activity-ktx:$activityVersion")

    implementation("androidx.navigation:navigation-runtime-ktx:$navVersion")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // https://developer.android.com/jetpack/androidx/releases/compose-kotlin
    // https://developer.android.com/jetpack/compose/bom/bom-mapping

    implementation(platform("androidx.compose:compose-bom:$bomVersion"))

    // Compose
    // Animation
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.compose.animation:animation-graphics")

    //Foundation
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")

    // Material
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // Runtime
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.runtime:runtime-livedata")

    // UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycleVersion")

    // Room
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$kotlinCoroutineVersion")

    //Dagger - Hilt
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("com.google.dagger:hilt-android:$daggerHiltVersion")
    ksp("com.google.dagger:hilt-compiler:$daggerHiltVersion")

    // Data store
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation("androidx.core:core-ktx:1.12.0")

    // Backport java.Time to SDK < 23
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Media Playback
    // For media playback using ExoPlayer
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    // For exposing and controlling media sessions
    implementation("androidx.media3:media3-session:$media3Version")

    // Testing
    androidTestImplementation(platform("androidx.compose:compose-bom:$bomVersion"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinCoroutineVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.mockk:mockk-agent:$mockkVersion")
    androidTestImplementation("io.mockk:mockk-android:$mockkVersion")
    androidTestImplementation("io.mockk:mockk-agent:$mockkVersion")

    // Local unit tests
    testImplementation("androidx.test:core:1.5.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")

    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("com.google.truth:truth:1.1.3")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.4")

    // Instrumentation tests
    androidTestImplementation("com.google.dagger:hilt-android-testing:$daggerHiltVersion")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:$daggerHiltVersion")
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinCoroutineVersion")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.4")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("com.google.truth:truth:1.1.3")
    androidTestImplementation("android.arch.persistence.room:testing:1.1.1")

}