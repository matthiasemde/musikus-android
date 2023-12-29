plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp")
    id("de.mannodermaus.android-junit5")
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "app.musikus"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.musikus"
        minSdk = 23
        targetSdk = compileSdk
        versionCode = 8
        versionName = "1.0.1"

        testInstrumentationRunner = "app.musikus.HiltTestRunner"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
//            arg("dagger.hilt.processor.moduleOutputDir", file("build/generated/source/ksp/modules"))
//            arg("dagger.hilt.processor.invokeTurbine", "true")
        }
    }

//    applicationVariants.configureEach { variant ->
//        variant.resValue "string", "versionName", "Version " + variant.versionName
//    }

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
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    testOptions { packagingOptions { jniLibs { useLegacyPackaging = true } } }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    buildFeatures {
        compose = true
//        viewBinding true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    val bomVersion = "2023.10.01"

    val lifecycleVersion = "2.6.2"
    val roomVersion = "2.6.1"
    val navVersion = "2.7.5"
    val daggerHiltVersion = "2.49"
    val kotlinCoroutineVersion = "1.7.3"
    val mockkVersion = "1.13.8"

    implementation("androidx.navigation:navigation-runtime-ktx:$navVersion")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")

    // https://developer.android.com/jetpack/androidx/releases/compose-kotlin
    // https://developer.android.com/jetpack/compose/setup#bom-version-mapping

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
    implementation("androidx.compose.material3:material3:1.2.0-beta01") // TODO remove explicit version once bom is updated
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

    // When using a MDC theme
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("com.google.android.material:compose-theme-adapter-3:1.1.1")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.17.0")

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
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("com.google.dagger:hilt-android:$daggerHiltVersion")
    ksp("com.google.dagger:hilt-compiler:$daggerHiltVersion")

    // Data store
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:$navVersion")
    implementation("androidx.navigation:navigation-ui-ktx:$navVersion")

    // Backport java.Time to SDK < 23
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // AppIntro
    implementation("com.github.AppIntro:AppIntro:6.2.0")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

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
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")

    // Instrumentation tests
    androidTestImplementation("com.google.dagger:hilt-android-testing:$daggerHiltVersion")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:$daggerHiltVersion")
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinCoroutineVersion")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("com.google.truth:truth:1.1.3")

}