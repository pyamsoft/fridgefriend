/*
 * Copyright 2019 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.pyamsoft.fridge.locator.map.permission

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat
import com.pyamsoft.fridge.locator.MapPermission
import com.pyamsoft.fridge.locator.permission.BackgroundLocationPermission
import com.pyamsoft.fridge.locator.permission.ForegroundLocationPermission
import com.pyamsoft.fridge.locator.permission.PermissionConsumer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PermissionGranter @Inject internal constructor(
    private val context: Context
) : MapPermission {

    @CheckResult
    private fun checkPermissions(permissions: Array<out String>): Boolean {
        return permissions.all { permission ->
            val permissionCheck = ContextCompat.checkSelfPermission(context, permission)
            return@all permissionCheck == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun hasForegroundPermission(): Boolean {
        return checkPermissions(ForegroundLocationPermission.permissions())
    }

    override fun requestForegroundPermission(consumer: PermissionConsumer<ForegroundLocationPermission>) {
        consumer.onRequestPermissions(
            ForegroundLocationPermission.permissions(),
            ForegroundLocationPermission.requestCode()
        )
    }

    override fun hasBackgroundPermission(): Boolean {
        if (VERSION.SDK_INT < VERSION_CODES.Q) {
            return true
        }

        return checkPermissions(BackgroundLocationPermission.permissions())
    }

    override fun requestBackgroundPermission(consumer: PermissionConsumer<BackgroundLocationPermission>) {
        if (VERSION.SDK_INT < VERSION_CODES.Q) {
            consumer.onPermissionResponse(BackgroundLocationPermission.asGrant(true))
            return
        }

        consumer.onRequestPermissions(
            BackgroundLocationPermission.permissions(),
            BackgroundLocationPermission.requestCode()
        )
    }
}
