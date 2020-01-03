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

import android.content.pm.PackageManager
import com.pyamsoft.fridge.locator.permission.Permission
import com.pyamsoft.fridge.locator.permission.PermissionConsumer
import com.pyamsoft.fridge.locator.permission.PermissionHandler
import javax.inject.Inject
import timber.log.Timber

internal class PermissionHandlerImpl<T : Permission> @Inject internal constructor(
    private val permission: T
) : PermissionHandler<T> {

    override fun handlePermissionResponse(
        consumer: PermissionConsumer<T>,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (permission.requestCode() != requestCode) {
            return
        }

        val permissionArray = permission.permissions()
        for (p in permissions) {
            if (p !in permissionArray) {
                Timber.w("Unknown permission found in response: $p")
                return consumer.onPermissionResponse(permission.asGrant(granted = false))
            }
        }

        consumer.onPermissionResponse(permission.asGrant(granted = grantResults.all {
            it == PackageManager.PERMISSION_GRANTED
        }))
    }
}
