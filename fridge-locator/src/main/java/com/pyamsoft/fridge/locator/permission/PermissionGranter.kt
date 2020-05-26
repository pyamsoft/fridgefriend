/*
 * Copyright 2020 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.locator.permission

import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat
import com.pyamsoft.fridge.locator.MapPermission
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    override suspend fun hasForegroundPermission(): Boolean =
        withContext(context = Dispatchers.Default) {
            checkPermissions(ForegroundLocationPermission.permissions())
        }

    override suspend fun requestForegroundPermission(consumer: PermissionConsumer<ForegroundLocationPermission>) =
        withContext(context = Dispatchers.Main) {
            consumer.onRequestPermissions(
                ForegroundLocationPermission.permissions(),
                ForegroundLocationPermission.requestCode()
            )
        }
}
