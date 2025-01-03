/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

// Store screenshots in "/sdcard/Android/media/app.musikus/additional_test_output"
// This way, the files will get automatically synced to app/build/outputs/managed_device_android_test_additional_output
// before the emulator gets shut down.
// Source: https://stackoverflow.com/questions/74069309/copy-data-from-an-android-emulator-that-is-run-by-gradle-managed-devices

class ScreenshotRule(
    private val composeTestRule: ComposeTestRule,
) : TestWatcher() {

    var outputDir: File

    init {
        @Suppress("Deprecation")
        outputDir = File(
            InstrumentationRegistry.getInstrumentation().targetContext.externalMediaDirs.first(),
            "additional_test_output"
        )

        // Ensure the directory exists
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
    }

    override fun failed(e: Throwable?, description: Description) {
        val testClassDir = File(
            outputDir,
            description.className
        )

        if (!testClassDir.exists()) {
            testClassDir.mkdirs()
        }

        val screenshotName = "${description.methodName}.png"
        val screenshotFile = File(testClassDir, screenshotName)
        Log.d("ComposeScreenshotRule", "Saving screenshot to ${screenshotFile.absolutePath}")

        // Capture the screenshot and save it
        composeTestRule.onRoot().captureToImage().asAndroidBitmap().apply {
            screenshotFile.outputStream().use { outputStream ->
                compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }

//        runBlocking { delay(100000) }
    }
}
