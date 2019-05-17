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

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.fragment.app.DialogFragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import com.pyamsoft.fridge.detail.item.fridge.DateSelectPayload
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireArguments
import java.util.Calendar
import javax.inject.Inject

internal class DatePickerDialogFragment : DialogFragment() {

  @JvmField @Inject internal var dateSelectBus: EventBus<DateSelectPayload>? = null

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    Injector.obtain<FridgeComponent>(requireContext().applicationContext)
        .inject(this)

    val today = Calendar.getInstance()
    val item: FridgeItem =
      requireNotNull(requireArguments().getParcelable<JsonMappableFridgeItem>(ITEM))

    var initialYear = requireArguments().getInt(YEAR, 0)
    var initialMonth = requireArguments().getInt(MONTH, 0)
    var initialDay = requireArguments().getInt(DAY, 0)
    if (initialYear == 0) {
      initialYear = today.get(Calendar.YEAR)
    }
    if (initialMonth == 0) {
      initialMonth = today.get(Calendar.MONTH)
    }
    if (initialDay == 0) {
      initialDay = today.get(Calendar.DAY_OF_MONTH)
    }

    return DatePickerDialog(
        requireActivity(),
        DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
          requireNotNull(dateSelectBus).publish(DateSelectPayload(item, year, month, dayOfMonth))
          dismiss()
        },
        initialYear, initialMonth, initialDay
    ).apply {
      datePicker.minDate = today.timeInMillis
    }
  }

  companion object {

    const val TAG = "DatePickerDialogFragment"
    private const val ITEM = "item"
    private const val YEAR = "year"
    private const val MONTH = "month"
    private const val DAY = "day"

    @JvmStatic
    @CheckResult
    fun newInstance(
      item: FridgeItem,
      year: Int,
      month: Int,
      day: Int
    ): DialogFragment {
      return DatePickerDialogFragment().apply {
        arguments = Bundle().apply {
          putParcelable(ITEM, JsonMappableFridgeItem.from(item))
          putInt(YEAR, year)
          putInt(MONTH, month)
          putInt(DAY, day)
        }
      }
    }

  }

}