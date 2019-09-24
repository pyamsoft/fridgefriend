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

import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import com.afollestad.assent.Permission
import com.afollestad.assent.Permission.ACCESS_COARSE_LOCATION
import com.afollestad.assent.Permission.ACCESS_FINE_LOCATION
import com.afollestad.assent.askForPermissions
import com.pyamsoft.fridge.locator.MapPermission.PermissionDenial

internal object PermissionManager {

    inline fun request(
        fragment: Fragment,
        permission: String,
        crossinline onGranted: () -> Unit,
        crossinline onDenied: () -> Unit
    ) {
        val perm = Permission.parse(permission)
        fragment.askForPermissions(perm) { result ->
            if (result.containsPermissions(perm)) {
                onGranted()
            } else {
                onDenied()
            }
        }
    }

    @CheckResult
    private fun Permission.toDenial(): PermissionDenial {
        return PermissionDenial(this.value)
    }

    inline fun request(
        fragment: Fragment,
        vararg permissions: String,
        crossinline onGranted: () -> Unit,
        crossinline onDenied: (denials: Collection<PermissionDenial>) -> Unit
    ) {
        val allPermissions = permissions.map { Permission.parse(it) }.toTypedArray()
        fragment.askForPermissions(*allPermissions) { result ->
            if (result.isAllGranted(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION)) {
                onGranted()
            } else {
                val denials = allPermissions
                    .filter { result.containsPermissions(it) }
                    .map { it.toDenial() }
                onDenied(denials)
            }
        }
    }
}
