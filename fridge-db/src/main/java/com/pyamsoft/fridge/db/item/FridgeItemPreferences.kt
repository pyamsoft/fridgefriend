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

package com.pyamsoft.fridge.db.item

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.core.PreferenceUnregister

interface FridgeItemPreferences {

    @CheckResult
    suspend fun isZeroCountConsideredConsumed(): Boolean

    @CheckResult
    suspend fun getExpiringSoonRange(): Int

    @CheckResult
    suspend fun watchForExpiringSoonChange(onChange: (newRange: Int) -> Unit): PreferenceUnregister

    @CheckResult
    suspend fun isSameDayExpired(): Boolean

    @CheckResult
    suspend fun watchForSameDayExpiredChange(onChange: (newSameDay: Boolean) -> Unit): PreferenceUnregister
}
