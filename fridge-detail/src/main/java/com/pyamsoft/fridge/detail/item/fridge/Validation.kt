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

package com.pyamsoft.fridge.detail.item.fridge

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.item.FridgeItem
import java.util.Calendar
import java.util.Date

@CheckResult
internal fun isNameValid(name: String): Boolean {
  return name.isNotBlank() && name != FridgeItem.EMPTY_NAME
}

@CheckResult
internal fun isDateValid(date: Date): Boolean {
  val calendar = Calendar.getInstance().apply { time = date }
  val year = calendar.get(Calendar.YEAR)
  val month = calendar.get(Calendar.MONTH)
  val day = calendar.get(Calendar.DAY_OF_MONTH)
  return isDateValid(year, month, day)
}

@CheckResult
internal fun isDateValid(year: Int, month: Int, day: Int): Boolean {
  val today = Calendar.getInstance()
  val maxMonthsInYear = today.getActualMaximum(Calendar.MONTH)
  val thisYear = today.get(Calendar.YEAR)
  val isMonthValid = month in 1..maxMonthsInYear
  val isYearValid = year in thisYear..(thisYear + 1000)

  if (isYearValid && isMonthValid) {
    val estimate = Calendar.getInstance().apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month)
    }
    val maxDaysInMonth = estimate.getActualMaximum(Calendar.DAY_OF_MONTH)
    val isDayValid: Boolean = day in 1..maxDaysInMonth

    if (isDayValid) {
      val date = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
      }
      return date.timeInMillis > 0 && date.after(today)
    }
  }

  return false
}
