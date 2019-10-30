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
import com.pyamsoft.fridge.core.Preferences.Unregister
import com.pyamsoft.fridge.db.FridgeItemPreferences
import com.pyamsoft.fridge.db.PersistentEntryPreferences
import com.pyamsoft.fridge.setting.SettingsPreferences
import com.pyamsoft.pydroid.core.Enforcer
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class PreferencesImpl @Inject internal constructor(
    private val enforcer: Enforcer,
    context: Context
) : ButlerPreferences,
    FridgeItemPreferences, PersistentEntryPreferences, SettingsPreferences {

    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    }

    private val expiringSoonKey: String
    private val expiringSoonDefault: String

    private val isSameDayExpiredKey: String
    private val isSameDayExpiredDefault: String

    init {
        val res = context.applicationContext.resources

        expiringSoonKey = res.getString(R.string.expiring_soon_range_key)
        expiringSoonDefault = res.getString(R.string.expiring_soon_range_default)

        isSameDayExpiredKey = res.getString(R.string.expired_same_day_key)
        isSameDayExpiredDefault = res.getString(R.string.expired_same_day_default)
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

    override fun getExpiringSoonRange(): Int {
        return preferences.getString(expiringSoonKey, expiringSoonDefault).orEmpty().toInt()
    }

    override fun isSameDayExpired(): Boolean {
        return preferences.getString(isSameDayExpiredKey, isSameDayExpiredDefault).orEmpty()
            .toBoolean()
    }

    override fun watchForExpiringSoonChange(onChange: (newRange: Int) -> Unit): Unregister {
        return registerPreferenceListener(OnSharedPreferenceChangeListener { _, key ->
            if (key == expiringSoonKey) {
                onChange(getExpiringSoonRange())
            }
        })
    }

    override fun watchForSameDayExpiredChange(onChange: (newSameDay: Boolean) -> Unit): Unregister {
        return registerPreferenceListener(OnSharedPreferenceChangeListener { _, key ->
            if (key == isSameDayExpiredKey) {
                onChange(isSameDayExpired())
            }
        })
    }

    @CheckResult
    private fun registerPreferenceListener(l: OnSharedPreferenceChangeListener): Unregister {
        preferences.registerOnSharedPreferenceChangeListener(l)
        var listener: OnSharedPreferenceChangeListener? = l
        return object : Unregister {

            override fun unregister() {
                listener?.let { preferences.unregisterOnSharedPreferenceChangeListener(it) }
                listener = null
            }
        }
    }

    override fun getPersistentId(key: String): String {
        return requireNotNull(preferences.getString(key, IdGenerator.generate()))
    }

    override fun savePersistentId(key: String, id: String) {
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
