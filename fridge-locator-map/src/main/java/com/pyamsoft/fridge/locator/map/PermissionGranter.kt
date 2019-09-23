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

package com.pyamsoft.fridge.locator.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.multi.BaseMultiplePermissionsListener
import com.karumi.dexter.listener.single.BasePermissionListener
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
        onDenied: (coarsePermanently: PermissionDenial?, finePermanently: PermissionDenial?) -> Unit
    ) {
        Dexter.withActivity(fragment.requireActivity())
            .withPermissions(COARSE_PERMISSION, FINE_PERMISSION)
            .withListener(object : BaseMultiplePermissionsListener() {

                // We need this in here otherwise, Dagger will not be able to find Dexter
                // using implementation.
                @CheckResult
                private fun PermissionDeniedResponse.toDenial(): PermissionDenial {
                    return PermissionDenial(this.permissionName, this.isPermanentlyDenied)
                }

                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if (report.areAllPermissionsGranted()) {
                        onGranted()
                    } else {
                        val denied = report.deniedPermissionResponses
                        val coarse =
                            denied.find { it.permissionName == COARSE_PERMISSION }
                        val fine = denied.find { it.permissionName == FINE_PERMISSION }
                        val coarseResponse = coarse?.toDenial()
                        val fineResponse = fine?.toDenial()
                        onDenied(coarseResponse, fineResponse)
                    }
                }
            }).check()
    }

    override fun hasBackgroundPermission(): Boolean {
        return if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            checkPermissions(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            true
        }
    }

    override fun requestBackgroundPermission(
        fragment: Fragment,
        onGranted: () -> Unit,
        onDenied: (permanently: Boolean) -> Unit
    ) {
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
            requestSinglePermission(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                fragment,
                onGranted,
                onDenied
            )
        } else {
            onGranted()
        }
    }

    override fun hasStoragePermission(): Boolean {
        return checkPermissions(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    override fun requestStoragePermission(
        fragment: Fragment,
        onGranted: () -> Unit,
        onDenied: (permanently: Boolean) -> Unit
    ) {
        requestSinglePermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            fragment,
            onGranted,
            onDenied
        )
    }

    private inline fun requestSinglePermission(
        permission: String,
        fragment: Fragment,
        crossinline onGranted: () -> Unit,
        crossinline onDenied: (permanently: Boolean) -> Unit
    ) {
        Dexter.withActivity(fragment.requireActivity())
            .withPermission(permission)
            .withListener(object : BasePermissionListener() {
                override fun onPermissionGranted(response: PermissionGrantedResponse) {
                    onGranted()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse) {
                    onDenied(response.isPermanentlyDenied)
                }
            }).check()
    }

    companion object {

        private const val COARSE_PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION
        private const val FINE_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    }
}
