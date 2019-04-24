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
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named

internal class DetailListItemDate @Inject internal constructor(
  @Named("item_editable") private val editable: Boolean,
  item: FridgeItem,
  parent: ViewGroup,
  callback: DetailListItemDate.Callback
) : DetailListItem<DetailListItemDate.Callback>(item, parent, callback) {

  override val layout: Int = R.layout.detail_list_item_date

  override val layoutRoot by boundView<ViewGroup>(R.id.detail_item_date)
  private val monthView by boundView<EditText>(R.id.detail_item_date_month)
  private val dayView by boundView<EditText>(R.id.detail_item_date_day)
  private val yearView by boundView<EditText>(R.id.detail_item_date_year)

  private var dateWatcher: TextWatcher? = null

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    if (item.expireTime() != FridgeItem.EMPTY_EXPIRE_TIME) {
      val date = Calendar.getInstance().apply {
        time = item.expireTime()
      }
      Timber.d("Expire time is: $date")

      // Month is zero indexed in storage
      val month = date.get(Calendar.MONTH) + 1
      val day = date.get(Calendar.DAY_OF_MONTH)
      val year = date.get(Calendar.YEAR)

      monthView.setTextKeepState("$month".padStart(2, '0'))
      dayView.setTextKeepState("$day".padStart(2, '0'))
      yearView.setTextKeepState("$year".padStart(4, '0'))
    }

    if (editable && !item.isArchived()) {
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

      yearView.setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_NULL) {
          callback.onLastDoneClicked()
          return@setOnEditorActionListener true
        }

        return@setOnEditorActionListener false
      }
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

    yearView.setOnEditorActionListener(null)

    // Cleaup
    monthView.text.clear()
    dayView.text.clear()
    yearView.text.clear()
  }

  override fun onMadeReal() {

  }

  private fun commit() {
    val month = monthView.text.toString()
    val day = dayView.text.toString()
    val year = yearView.text.toString()
    val yearNumber = if (year.isBlank()) 0 else year.toInt()
    val monthNumber = if (month.isBlank()) 0 else month.toInt()
    val dayNumber = if (day.isBlank()) 0 else day.toInt()
    callback.commitDate(item, yearNumber, monthNumber, dayNumber)
  }

  interface Callback : DetailListItem.Callback {

    fun onLastDoneClicked()

    fun commitDate(oldItem: FridgeItem, year: Int, month: Int, day: Int)

  }

}

