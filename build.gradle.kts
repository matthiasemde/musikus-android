// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.3.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
    id("com.jaredsburrows.license") version "0.9.7" apply false
    id("androidx.room") version "2.6.1" apply false
}

buildscript {
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.49")
        classpath("de.mannodermaus.gradle.plugins:android-junit5:1.10.0.0")
    }
}
