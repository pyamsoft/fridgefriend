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

import dagger.Binds
import dagger.Module

@Module
abstract class PreferenceModule {

  @Binds internal abstract fun bindNeededPreference(impl: PreferencesImpl): NeededPreferences

  @Binds internal abstract fun bindNightlyPreference(impl: PreferencesImpl): NightlyPreferences

  @Binds internal abstract fun bindExpiringPreference(impl: PreferencesImpl): ExpiringPreferences

  @Binds internal abstract fun bindExpiredPreference(impl: PreferencesImpl): ExpiredPreferences

  @Binds internal abstract fun bindSearchPreferences(impl: PreferencesImpl): SearchPreferences

  @Binds
  internal abstract fun bindNotificationPreferences(impl: PreferencesImpl): NotificationPreferences

  @Binds internal abstract fun bindDetailPreferences(impl: PreferencesImpl): DetailPreferences

  @Binds
  internal abstract fun bindPersistentCategoryPreferences(
      impl: PreferencesImpl
  ): PersistentPreferences

  @Binds internal abstract fun bindSettingsPreferences(impl: PreferencesImpl): SettingsPreferences

  @Binds internal abstract fun bindEntryPreferences(impl: PreferencesImpl): EntryPreferences
}
