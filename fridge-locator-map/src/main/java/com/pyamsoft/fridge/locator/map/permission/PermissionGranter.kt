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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.pyamsoft.fridge.locator.MapPermission
import com.pyamsoft.fridge.locator.MapPermission.PermissionDenial
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PermissionGranter @Inject internal constructor(
    private val context: Context
) : MapPermission {

    @CheckResult
    private fun checkPermissions(vararg permissions: String): Boolean {
        return permissions.all { permission ->
            val permissionCheck = ContextCompat.checkSelfPermission(context, permission)
            return@all permissionCheck == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun hasForegroundPermission(): Boolean {
        return checkPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    override fun requestForegroundPermission(
        fragment: Fragment,
        onGranted: () -> Unit,
        onDenied: (coarseDenied: PermissionDenial?, fineDenied: PermissionDenial?) -> Unit
    ) {
        PermissionManager.request(
            fragment,
            COARSE_PERMISSION,
            FINE_PERMISSION,
            onGranted = onGranted,
            onDenied = { denials ->
                val coarseDenied = denials.find { it.permission == COARSE_PERMISSION }
                val fineDenied = denials.find { it.permission == FINE_PERMISSION }
                onDenied(coarseDenied, fineDenied)
            })
    }

    override fun hasBackgroundPermission(): Boolean {
        if (VERSION.SDK_INT < VERSION_CODES.Q) {
            return true
        }

        return checkPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    override fun requestBackgroundPermission(
        fragment: Fragment,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        if (VERSION.SDK_INT < VERSION_CODES.Q) {
            onGranted()
            return
        }

        PermissionManager.request(
            fragment,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            onGranted,
            onDenied
        )
    }

    companion object {

        private const val COARSE_PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION
        private const val FINE_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    }
}
