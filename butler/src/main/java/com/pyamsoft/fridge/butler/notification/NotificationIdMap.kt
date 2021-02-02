/*
 * Copyright 2021 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.fridge.butler.notification

import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.notify.NotifyId
import com.pyamsoft.pydroid.notify.toNotifyId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NotificationIdMap @Inject internal constructor() {

    private val map by lazy { mutableMapOf<NotificationType, IdMap>() }

    @CheckResult
    fun getNotifications(type: NotificationType): IdMap {
        return map.getOrPut(type) { mutableMapOf() }
    }

    @CheckResult
    inline fun getNotificationId(
        type: NotificationType,
        key: () -> String
    ): NotifyId {
        val k = key()
        return getNotifications(type).getOrPut(k) { (k.hashCode() + type.ordinal).toNotifyId() }
    }
}

typealias IdMap = MutableMap<String, NotifyId>
