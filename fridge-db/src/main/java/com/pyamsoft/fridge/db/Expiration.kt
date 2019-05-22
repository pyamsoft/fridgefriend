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

package com.pyamsoft.fridge.db

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.item.FridgeItem
import java.util.Calendar

@CheckResult
fun Calendar.atMidnight(): Calendar {
  return this.apply {
    set(Calendar.HOUR, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }
}

@CheckResult
fun Calendar.tomorrowMidnight(): Calendar {
  return this.apply {
    add(Calendar.DAY_OF_MONTH, 1)
  }
      .atMidnight()
}

@CheckResult
fun FridgeItem.isExpired(today: Calendar): Boolean {
  // Clean Y/M/D only
  val expiration = Calendar.getInstance()
      .also {
        it.time = this.expireTime()
      }
      .atMidnight()

  val midnightToday = today.atMidnight()
  return expiration.before(midnightToday)
}

@CheckResult
fun FridgeItem.isExpiringSoon(
  today: Calendar,
  tomorrow: Calendar
): Boolean {
  // Clean Y/M/D only
  val expiration = Calendar.getInstance()
      .also {
        it.time = this.expireTime()
      }
      .atMidnight()

  val midnightToday = today.atMidnight()
  val midnightTomorrow = tomorrow.atMidnight()
  return expiration.before(
      midnightTomorrow
  ) || expiration == midnightTomorrow || expiration == midnightToday
}


