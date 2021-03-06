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

package com.pyamsoft.fridge.preference

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.util.PreferenceListener
import com.pyamsoft.pydroid.util.onChange
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
internal class PreferencesImpl
@Inject
internal constructor(
    context: Context,
) :
    NeededPreferences,
    ExpiringPreferences,
    ExpiredPreferences,
    NightlyPreferences,
    DetailPreferences,
    PersistentPreferences,
    SettingsPreferences,
    NotificationPreferences,
    EntryPreferences,
    SearchPreferences {

  private val preferences by lazy {
    Enforcer.assertOffMainThread()
    PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
  }

  // String instead of Int because ListPreference only holds Strings -_-
  private val expiringSoonDefault: String
  private val expiringSoonKey: String

  // String instead of Boolean because ListPreference only holds Strings -_-
  private val isSameDayExpiredDefault: String
  private val isSameDayExpiredKey: String

  // String instead of Int because ListPreference only holds Strings -_-
  private val notificationPeriodDefault: String
  private val notificationPeriodKey: String

  private val doNotDisturbDefault: Boolean
  private val doNotDisturbKey: String

  private val isZeroCountConsumedDefault: Boolean
  private val isZeroCountConsumedKey: String

  private val searchEmptyStateDefault: Boolean
  private val searchEmptyStateKey: String

  private var isOldCleared = false

  init {
    context.applicationContext.resources.apply {
      expiringSoonKey = getString(R.string.expiring_soon_range_key)
      expiringSoonDefault = getString(R.string.expiring_soon_range_default)

      isSameDayExpiredKey = getString(R.string.expired_same_day_key)
      isSameDayExpiredDefault = getString(R.string.expired_same_day_default)

      doNotDisturbKey = getString(R.string.do_not_disturb_key)
      doNotDisturbDefault = getBoolean(R.bool.do_not_disturb_default)

      notificationPeriodKey = getString(R.string.notification_period_range_key)
      notificationPeriodDefault = getString(R.string.notification_period_range_default)

      isZeroCountConsumedKey = getString(R.string.zero_count_consumed_key)
      isZeroCountConsumedDefault = getBoolean(R.bool.zero_count_consumed_default)

      searchEmptyStateKey = getString(R.string.search_empty_state_key)
      searchEmptyStateDefault = getBoolean(R.bool.search_empty_state_default)
    }
  }

  private suspend fun clearOldPreferences() =
      withContext(context = Dispatchers.IO) {
        if (isOldCleared) {
          return@withContext
        }

        isOldCleared = true
        preferences.edit { remove(KEY_PERSISTENT_ENTRIES) }
      }

  override suspend fun isQuickAddEnabled(): Boolean {
    // TODO(Peter): Implement
    clearOldPreferences()
    return true
  }

  override suspend fun isEmptyStateAllItems(): Boolean =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        return@withContext preferences.getBoolean(searchEmptyStateKey, searchEmptyStateDefault)
      }

  override suspend fun getLastNotificationTimeExpiringSoon(): Long =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        return@withContext preferences.getLong(KEY_LAST_NOTIFICATION_TIME_EXPIRING_SOON, 0)
      }

  override suspend fun markNotificationExpiringSoon(calendar: Calendar) =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        preferences.edit {
          putLong(KEY_LAST_NOTIFICATION_TIME_EXPIRING_SOON, calendar.timeInMillis)
        }
      }

  override suspend fun getLastNotificationTimeExpired(): Long =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        return@withContext preferences.getLong(KEY_LAST_NOTIFICATION_TIME_EXPIRED, 0)
      }

  override suspend fun markNotificationExpired(calendar: Calendar) =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        preferences.edit { putLong(KEY_LAST_NOTIFICATION_TIME_EXPIRED, calendar.timeInMillis) }
      }

  override suspend fun getLastNotificationTimeNeeded(): Long =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        return@withContext preferences.getLong(KEY_LAST_NOTIFICATION_TIME_NEEDED, 0)
      }

  override suspend fun markNotificationNeeded(calendar: Calendar) =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        preferences.edit { putLong(KEY_LAST_NOTIFICATION_TIME_NEEDED, calendar.timeInMillis) }
      }

  override suspend fun getLastNotificationTimeNightly(): Long =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        return@withContext preferences.getLong(KEY_LAST_NOTIFICATION_TIME_NIGHTLY, 0)
      }

  override suspend fun markNotificationNightly(calendar: Calendar) =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        preferences.edit { putLong(KEY_LAST_NOTIFICATION_TIME_NIGHTLY, calendar.timeInMillis) }
      }

  override suspend fun getExpiringSoonRange(): Int =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        return@withContext preferences
            .getString(expiringSoonKey, expiringSoonDefault)
            ?.toIntOrNull()
            ?: FALLBACK_EXPIRING_SOON
      }

  override suspend fun isSameDayExpired(): Boolean =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        return@withContext preferences
            .getString(isSameDayExpiredKey, isSameDayExpiredDefault)
            ?.toBoolean()
            ?: FALLBACK_SAME_DAY_EXPIRED
      }

  override suspend fun isZeroCountConsideredConsumed(): Boolean =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        return@withContext preferences.getBoolean(
            isZeroCountConsumedKey, isZeroCountConsumedDefault)
      }

  override suspend fun isDoNotDisturb(now: Calendar): Boolean =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        return@withContext if (!preferences.getBoolean(doNotDisturbKey, doNotDisturbDefault)) false
        else {
          val currentHour = now.get(Calendar.HOUR_OF_DAY)
          currentHour < 7 || currentHour >= 22
        }
      }

  override suspend fun watchForExpiringSoonChange(
      onChange: (newRange: Int) -> Unit
  ): PreferenceListener =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        return@withContext preferences.onChange(expiringSoonKey) {
          onChange(getExpiringSoonRange())
        }
      }

  override suspend fun watchForSameDayExpiredChange(
      onChange: (newSameDay: Boolean) -> Unit
  ): PreferenceListener =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        return@withContext preferences.onChange(isSameDayExpiredKey) {
          onChange(isSameDayExpired())
        }
      }

  override suspend fun watchForSearchEmptyStateChange(
      onChange: (Boolean) -> Unit
  ): PreferenceListener =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        return@withContext preferences.onChange(searchEmptyStateKey) {
          onChange(isEmptyStateAllItems())
        }
      }

  override suspend fun getNotificationPeriod(): Long =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        val hours =
            preferences.getString(notificationPeriodKey, notificationPeriodDefault)?.toLongOrNull()
                ?: FALLBACK_NOTIFICATION_PERIOD
        return@withContext TimeUnit.HOURS.toMillis(hours)
      }

  override suspend fun canNotify(now: Calendar, lastNotificationTime: Long): Boolean =
      withContext(context = Dispatchers.IO) {
        clearOldPreferences()
        return@withContext lastNotificationTime + getNotificationPeriod() < now.timeInMillis
      }

  override suspend fun clear() =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        preferences.edit(commit = true) { clear() }
      }

  override suspend fun isPersistentCategoriesInserted(): Boolean =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        return@withContext preferences.getBoolean(KEY_PERSISTENT_CATEGORIES, false)
      }

  override suspend fun setPersistentCategoriesInserted() =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        preferences.edit { putBoolean(KEY_PERSISTENT_CATEGORIES, true) }
      }

  override suspend fun isDefaultEntryCreated(): Boolean =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        return@withContext preferences.getBoolean(KEY_ENTRY_DEFAULT_CREATED, false)
      }

  override suspend fun markDefaultEntryCreated() =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        clearOldPreferences()
        preferences.edit { putBoolean(KEY_ENTRY_DEFAULT_CREATED, true) }
      }

  companion object {

    private const val FALLBACK_EXPIRING_SOON = 1
    private const val FALLBACK_SAME_DAY_EXPIRED = false
    private const val FALLBACK_NOTIFICATION_PERIOD = 2L

    private const val KEY_PERSISTENT_CATEGORIES = "persistent_categories_v1"
    private const val KEY_PERSISTENT_ENTRIES = "persistent_entries_v1"

    private const val KEY_LAST_NOTIFICATION_TIME_EXPIRING_SOON =
        "last_notification_expiring_soon_v1"
    private const val KEY_LAST_NOTIFICATION_TIME_EXPIRED = "last_notification_expired_v1"
    private const val KEY_LAST_NOTIFICATION_TIME_NEEDED = "last_notification_needed_v1"
    private const val KEY_LAST_NOTIFICATION_TIME_NIGHTLY = "last_notification_nightly_v1"
    private const val KEY_ENTRY_DEFAULT_CREATED = "default_entry_created_v1"
  }
}
