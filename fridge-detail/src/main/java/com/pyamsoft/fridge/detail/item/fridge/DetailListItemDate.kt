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

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named

internal class DetailListItemDate @Inject internal constructor(
  @Named("item_editable") private val editable: Boolean,
  item: FridgeItem,
  parent: ViewGroup,
  callback: Callback
) : DetailListItem(item, parent, callback) {

  override val layout: Int = R.layout.detail_list_item_date

  override val layoutRoot by lazyView<ViewGroup>(R.id.detail_item_date)
  private val monthView by lazyView<EditText>(R.id.detail_item_date_month)
  private val dayView by lazyView<EditText>(R.id.detail_item_date_day)
  private val yearView by lazyView<EditText>(R.id.detail_item_date_year)

  private var dateWatcher: TextWatcher? = null

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    val date = Calendar.getInstance().apply {
      time = item.expireTime()
    }
    val month = date.get(Calendar.MONTH)
    val day = date.get(Calendar.DAY_OF_MONTH)
    val year = date.get(Calendar.YEAR)

    monthView.setTextKeepState("$month")
    dayView.setTextKeepState("$day")
    yearView.setTextKeepState("$year")

    if (editable) {
      val watcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
          commit()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }
      }

      monthView.addTextChangedListener(watcher)
      dayView.addTextChangedListener(watcher)
      yearView.addTextChangedListener(watcher)
      dateWatcher = watcher
    } else {
      monthView.setNotEditable()
      dayView.setNotEditable()
      yearView.setNotEditable()
    }
  }

  override fun onTeardown() {
    // Unbind all listeners
    dateWatcher?.let { watcher ->
      monthView.removeTextChangedListener(watcher)
      dayView.removeTextChangedListener(watcher)
      yearView.removeTextChangedListener(watcher)
    }
    dateWatcher = null

    // Cleaup
    monthView.text.clear()
    dayView.text.clear()
    yearView.text.clear()
  }

  private fun commit() {
    val month = monthView.text.toString()
    val day = dayView.text.toString()
    val year = yearView.text.toString()
    if (month.isNotBlank() && day.isNotBlank() && year.isNotBlank()) {
      val date = Calendar.getInstance().apply {
        set(Calendar.YEAR, year.toInt())
        set(Calendar.MONTH, month.toInt())
        set(Calendar.DAY_OF_MONTH, day.toInt())
      }
      commitModel(expireTime = date.time)
    }
  }

}

