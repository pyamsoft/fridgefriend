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

package com.pyamsoft.fridge.detail

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.core.Preferences.Unregister
import com.pyamsoft.fridge.db.FridgeItemPreferences

abstract class DetailPreferenceInteractor protected constructor(
    private val preferences: FridgeItemPreferences
) {

    @CheckResult
    fun getExpiringSoonRange(): Int {
        return preferences.getExpiringSoonRange()
    }

    @CheckResult
    fun isSameDayExpired(): Boolean {
        return preferences.isSameDayExpired()
    }

    @CheckResult
    fun watchForExpiringSoonChanges(onChange: (newRange: Int) -> Unit): Unregister {
        return preferences.watchForExpiringSoonChange(onChange)
    }

    @CheckResult
    fun watchForSameDayExpiredChange(onChange: (newSameDayExpired: Boolean) -> Unit): Unregister {
        return preferences.watchForSameDayExpiredChange(onChange)
    }
}