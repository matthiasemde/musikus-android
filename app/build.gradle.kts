@file:Suppress("UnstableApiUsage")

import java.util.Properties
import java.util.Scanner

val properties = Properties()
file("$rootDir/build.properties").inputStream().use { properties.load(it) }
val importedVersionCode = properties["versionCode"] as String
val importedVersionName = properties["versionName"] as String
val commitHash = properties["commitHash"] as String

val reportsPath = "$projectDir/build/reports"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.junit5)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.license.report)
    alias(libs.plugins.detekt)
}

android {
    val javaVersion = JavaVersion.VERSION_17

    namespace = "app.musikus"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.musikus"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = importedVersionCode.toInt()
        versionName = importedVersionName

        base.archivesName = "$applicationId-v$versionName"

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
        } catch (e: Exception) {
            logger.warn("No signing configuration found, using debug key (message: ${e.message})")
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

    lint {
        lintConfig = file("$projectDir/config/androidLint.xml")

//        warningsAsErrors = true
//        abortOnError = true

        htmlOutput = file("$reportsPath/lint/android.html")
        xmlOutput = file("$reportsPath/lint/android.xml")
        textOutput = file("$reportsPath/lint/android.txt")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

detekt {
    // Version of detekt that will be used. When unspecified the latest detekt
    // version found will be used. Override to stay on the same version.
    toolVersion = "1.23.6"

    // Point to your custom config defining rules to run, overwriting default behavior
    config.setFrom("$projectDir/config/detekt.yml")

    // Applies the config files on top of detekt's default config file. `false` by default.
    buildUponDefaultConfig = true

    // Specify the base path for file paths in the formatted reports.
    // If not set, all file paths reported will be absolute file path.
    basePath = "$reportsPath/lint"
}

tasks.withType<Test> {
    useJUnitPlatform()
    reports {
        html.required = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        if (project.findProperty("composeCompilerReports") == "true") {
            freeCompilerArgs.addAll(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=$reportsPath/composeCompiler",
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=$reportsPath/composeCompiler"
            )
        }
    }
}

tasks.register("setupMusikus") {
    group = "setup"
    description = "Sets up the Musikus project with pre-commit hooks, copyright header, and IDE settings."

    doFirst {
        gradle.startParameter.consoleOutput = ConsoleOutput.Plain
    }

    doLast {
        println("Setting up Musikus project...")

        // Step 1: Execute the existing bash script to install the pre-commit hook
        if (System.getProperty("os.name").lowercase().contains("win")) {
            exec {
                workingDir = file("../.github/hooks")
                commandLine("cmd", "/c", "setup_hooks.bat")
            }
        } else {
            exec {
                commandLine("bash", ".github/hooks/setup_hooks.sh")
            }
        }
        println("Pre-commit hook installed.")

        // Step 2: Query and store a name for the copyright header
        val scanner = Scanner(System.`in`)
        print("Enter your name for the copyright header: ")
        System.out.flush() // Needed to ensure the prompt is displayed
        val name = scanner.nextLine()
        require(!name.isNullOrBlank()) { "Name must not be empty." }
        val propertiesFile = file("$rootDir/musikus.properties")
        propertiesFile.writeText("copyrightName=$name")
        println("Name stored for copyright header: $name")
    }
}

dependencies {
    detektPlugins(libs.detekt.formatting)

    // BOM
    // https://developer.android.com/jetpack/androidx/releases/compose-kotlin
    // https://developer.android.com/jetpack/compose/bom/bom-mapping
    implementation(platform(libs.androidx.compose.bom))

    // Core
    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.activity.ktx)

    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.legacy.support.v4)

    // Compose
    // Animation
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.animation.graphics)

    // Foundation
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)

    // Material
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowsizeclass)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    // Runtime
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.livedata)

    // UI
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Immutable Lists
    implementation(libs.kotlinx.collections.immutable)

    // Dagger - Hilt
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Data store
    implementation(libs.androidx.datastore.preferences)

    // Backport java.Time to SDK < 23
    coreLibraryDesugaring(libs.core.jdk.desugaring)

    // Media Playback
    // For media playback using ExoPlayer
    implementation(libs.androidx.media3.exoplayer)
    // For exposing and controlling media sessions
    implementation(libs.androidx.media3.session)

    // Testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.mockk.agent)

    // Unit tests
    testImplementation(libs.androidx.test.core)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    testImplementation(libs.androidx.core.testing)
    testImplementation(libs.google.truth)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Instrumentation tests
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.core.testing)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.google.truth)
    androidTestImplementation(libs.android.arch.persistence.room.testing)
}
