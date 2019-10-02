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

package com.pyamsoft.fridge.preference

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.pyamsoft.fridge.butler.ButlerPreferences
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PreferencesImpl @Inject internal constructor(context: Context) : ButlerPreferences {

    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    }

    override fun getLastNotificationTimeNearby(): Long {
        return preferences.getLong(KEY_LAST_NOTIFICATION_TIME_NEARBY, 0)
    }

    override fun markNotificationNearby(calendar: Calendar) {
        preferences.edit {
            putLong(KEY_LAST_NOTIFICATION_TIME_NEARBY, calendar.timeInMillis)
        }
    }

    override fun getLastNotificationTimeExpiringSoon(): Long {
        return preferences.getLong(KEY_LAST_NOTIFICATION_TIME_EXPIRING_SOON, 0)
    }

    override fun markNotificationExpiringSoon(calendar: Calendar) {
        preferences.edit {
            putLong(KEY_LAST_NOTIFICATION_TIME_EXPIRING_SOON, calendar.timeInMillis)
        }
    }

    override fun getLastNotificationTimeExpired(): Long {
        return preferences.getLong(KEY_LAST_NOTIFICATION_TIME_EXPIRED, 0)
    }

    override fun markNotificationExpired(calendar: Calendar) {
        preferences.edit {
            putLong(KEY_LAST_NOTIFICATION_TIME_EXPIRED, calendar.timeInMillis)
        }
    }

    companion object {

        private const val KEY_LAST_NOTIFICATION_TIME_NEARBY = "last_notification_nearby_v1"
        private const val KEY_LAST_NOTIFICATION_TIME_EXPIRING_SOON =
            "last_notification_expiring_soon_v1"
        private const val KEY_LAST_NOTIFICATION_TIME_EXPIRED = "last_notification_expired_v1"
    }
}
