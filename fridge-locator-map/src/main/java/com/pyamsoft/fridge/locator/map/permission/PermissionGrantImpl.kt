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
import com.pyamsoft.fridge.locator.permission.Permission
import com.pyamsoft.fridge.locator.permission.PermissionGrant

internal data class PermissionGrantImpl<T : Permission> internal constructor(
    private val permission: T,
    private val granted: Boolean
) : PermissionGrant<T> {

    override fun permission(): T {
        return permission
    }

    override fun granted(): Boolean {
        return granted
    }
}

@CheckResult
internal fun <T : Permission> T.asGrant(granted: Boolean): PermissionGrant<T> {
    return PermissionGrantImpl(this, granted)
}
