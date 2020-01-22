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
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.annotation.CheckResult
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.core.IdGenerator
import com.pyamsoft.fridge.core.PreferenceUnregister
import com.pyamsoft.fridge.db.FridgeItemPreferences
import com.pyamsoft.fridge.db.PersistentEntryPreferences
import com.pyamsoft.fridge.setting.SettingsPreferences
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PreferencesImpl @Inject internal constructor(
    private val enforcer: Enforcer,
    context: Context
) : ButlerPreferences, FridgeItemPreferences, PersistentEntryPreferences, SettingsPreferences {

    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    }

    private val expiringSoonKey: String
    private val expiringSoonDefault: String

    private val isSameDayExpiredKey: String
    private val isSameDayExpiredDefault: String

    init {
        context.applicationContext.resources.apply {
            expiringSoonKey = getString(R.string.expiring_soon_range_key)
            expiringSoonDefault = getString(R.string.expiring_soon_range_default)

            isSameDayExpiredKey = getString(R.string.expired_same_day_key)
            isSameDayExpiredDefault = getString(R.string.expired_same_day_default)
        }
    }

    override suspend fun getLastNotificationTimeNearby(): Long {
        enforcer.assertNotOnMainThread()
        return preferences.getLong(KEY_LAST_NOTIFICATION_TIME_NEARBY, 0)
    }

    override suspend fun markNotificationNearby(calendar: Calendar) {
        enforcer.assertNotOnMainThread()
        preferences.edit {
            putLong(KEY_LAST_NOTIFICATION_TIME_NEARBY, calendar.timeInMillis)
        }
    }

    override suspend fun getLastNotificationTimeExpiringSoon(): Long {
        enforcer.assertNotOnMainThread()
        return preferences.getLong(KEY_LAST_NOTIFICATION_TIME_EXPIRING_SOON, 0)
    }

    override suspend fun markNotificationExpiringSoon(calendar: Calendar) {
        enforcer.assertNotOnMainThread()
        preferences.edit {
            putLong(KEY_LAST_NOTIFICATION_TIME_EXPIRING_SOON, calendar.timeInMillis)
        }
    }

    override suspend fun getLastNotificationTimeExpired(): Long {
        enforcer.assertNotOnMainThread()
        return preferences.getLong(KEY_LAST_NOTIFICATION_TIME_EXPIRED, 0)
    }

    override suspend fun markNotificationExpired(calendar: Calendar) {
        enforcer.assertNotOnMainThread()
        preferences.edit {
            putLong(KEY_LAST_NOTIFICATION_TIME_EXPIRED, calendar.timeInMillis)
        }
    }

    override suspend fun getExpiringSoonRange(): Int {
        enforcer.assertNotOnMainThread()
        return preferences.getString(expiringSoonKey, expiringSoonDefault).orEmpty().toInt()
    }

    override suspend fun isSameDayExpired(): Boolean {
        enforcer.assertNotOnMainThread()
        return preferences.getString(isSameDayExpiredKey, isSameDayExpiredDefault).orEmpty()
            .toBoolean()
    }

    override suspend fun watchForExpiringSoonChange(onChange: (newRange: Int) -> Unit): PreferenceUnregister {
        return withContext(context = Dispatchers.Default) {
            registerPreferenceListener(OnSharedPreferenceChangeListener { _, key ->
                if (key == expiringSoonKey) {
                    launch(context = Dispatchers.Default) {
                        onChange(getExpiringSoonRange())
                    }
                }
            })
        }
    }

    override suspend fun watchForSameDayExpiredChange(onChange: (newSameDay: Boolean) -> Unit): PreferenceUnregister {
        return withContext(context = Dispatchers.Default) {
            registerPreferenceListener(OnSharedPreferenceChangeListener { _, key ->
                if (key == isSameDayExpiredKey) {
                    launch(context = Dispatchers.Default) {
                        onChange(isSameDayExpired())
                    }
                }
            })
        }
    }

    @CheckResult
    private fun registerPreferenceListener(l: OnSharedPreferenceChangeListener): PreferenceUnregister {
        enforcer.assertNotOnMainThread()
        preferences.registerOnSharedPreferenceChangeListener(l)

        return object : PreferenceUnregister {

            override fun unregister() {
                preferences.unregisterOnSharedPreferenceChangeListener(l)
            }
        }
    }

    override suspend fun getPersistentId(key: String): String {
        enforcer.assertNotOnMainThread()
        return requireNotNull(preferences.getString(key, IdGenerator.generate()))
    }

    override suspend fun savePersistentId(key: String, id: String) {
        enforcer.assertNotOnMainThread()
        preferences.edit { putString(key, id) }
    }

    override suspend fun clear() {
        enforcer.assertNotOnMainThread()
        preferences.edit(commit = true) {
            clear()
        }
    }

    companion object {

        private const val KEY_LAST_NOTIFICATION_TIME_NEARBY = "last_notification_nearby_v1"
        private const val KEY_LAST_NOTIFICATION_TIME_EXPIRING_SOON =
            "last_notification_expiring_soon_v1"
        private const val KEY_LAST_NOTIFICATION_TIME_EXPIRED = "last_notification_expired_v1"
    }
}
