/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

package app

import android.os.Build
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class MinApiVersion(val value: Int)

class MinApiVersionRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        val annotation = description.getAnnotation(MinApiVersion::class.java)
        return if (annotation != null && Build.VERSION.SDK_INT < annotation.value) {
            object : Statement() {
                override fun evaluate() {
                    println("Test skipped for API below ${annotation.value}")
                }
            }
        } else {
            base
        }
    }
}
