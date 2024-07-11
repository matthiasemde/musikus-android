/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024 Matthias Emde
 */

/**
 * https://medium.com/@vuert/handling-android-permissions-in-repository-9c062b4ea85e
 */

package app.musikus.permissions.domain

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import app.musikus.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

private typealias PermissionResult = Map<String, Boolean>

abstract class PermissionCheckerActivity : ComponentActivity() {
    abstract var permissionChecker: PermissionChecker

    lateinit var resultLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resultLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { result: PermissionResult ->
            permissionChecker.onPermissionResult(result)
        }
        permissionChecker.attachToActivity(this)
    }
}

class PermissionChecker(
    private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope
) {

    private lateinit var activity: PermissionCheckerActivity

    private val permissionCheckLock = Mutex()
    private var resultCallback: ((PermissionResult) -> Unit)? = null

    fun onPermissionResult(result: PermissionResult) {
        resultCallback?.invoke(result)
    }

    fun attachToActivity(activity: PermissionCheckerActivity) {
        this.activity = activity
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun requestPermission(vararg permissions: String): Result<Unit> {
        if (!this::activity.isInitialized) {
            return Result.failure(IllegalStateException("No activity available"))
        }

        // if there are no permissions requested, or all permissions are already granted, return success
        if (permissions.isEmpty() || permissions.all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }) {
            return Result.success(Unit)
        }

        permissionCheckLock.lock()

        val result =  try {
            val result: PermissionResult = withContext(applicationScope.coroutineContext) {
                val result: PermissionResult = suspendCancellableCoroutine { coroutine ->
                    resultCallback = { permissionResult ->
                        coroutine.resume(
                            value = permissionResult,
                            onCancellation = null
                        )
                    }
                    coroutine.invokeOnCancellation { resultCallback = null }
                    activity.resultLauncher.launch(arrayOf(*permissions))
//                    activity = null // Preventing memory leak
                }

                resultCallback = null

                result
            }

            if (result.all { it.value }) {
                Result.success(Unit)
            } else {
                val deniedPermissions = result
                    .entries
                    .asSequence()
                    .filter { !it.value }
                    .map { it.key }
                    .toSet()

                Result.failure(PermissionsDeniedException(deniedPermissions))
            }
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        } finally {
            permissionCheckLock.unlock()
        }

        return result
    }
    class PermissionsDeniedException(val permissions: Set<String>) : Exception()
}