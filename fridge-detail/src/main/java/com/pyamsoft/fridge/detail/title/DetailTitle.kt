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

package com.pyamsoft.fridge.detail.title

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.textfield.TextInputLayout
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.list.DetailListViewEvent
import com.pyamsoft.fridge.detail.list.DetailListViewEvent.NameUpdate
import com.pyamsoft.fridge.detail.list.DetailListViewState
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.Snackbreak
import java.text.SimpleDateFormat
import javax.inject.Inject

class DetailTitle @Inject internal constructor(
  private val owner: LifecycleOwner,
  parent: ViewGroup
) : BaseUiView<DetailListViewState, DetailListViewEvent>(parent) {

  override val layout: Int = R.layout.detail_title

  override val layoutRoot by boundView<ViewGroup>(R.id.entry_detail_title)

  private val name by boundView<TextInputLayout>(R.id.entry_detail_title_name)
  private val date by boundView<TextView>(R.id.entry_detail_title_date)
  private val count by boundView<TextView>(R.id.entry_detail_title_count)

  private var watcher: TextWatcher? = null

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    name.requestFocus()
    addTextWatcher()
  }

  private fun addTextWatcher() {
    requireNotNull(name.editText).let {
      watcher = object : TextWatcher {

        override fun onTextChanged(
          s: CharSequence?,
          start: Int,
          before: Int,
          count: Int
        ) {
        }

        override fun beforeTextChanged(
          s: CharSequence?,
          start: Int,
          count: Int,
          after: Int
        ) {
        }

        override fun afterTextChanged(s: Editable?) {
          if (s != null) {
            publish(NameUpdate(s.toString()))
          }
        }

      }
      it.addTextChangedListener(watcher)
    }
  }

  private fun removeTextWatcher() {
    watcher?.let { name.editText?.removeTextChangedListener(it) }
    watcher = null
  }

  override fun onTeardown() {
    removeTextWatcher()
    clearError()
    name.clearFocus()
    name.editText?.text?.clear()
    name.editText?.clearFocus()
  }

  private fun updateName(newName: String) {
    removeTextWatcher()
    val editText = requireNotNull(name.editText)
    editText.setTextKeepState(newName)
    addTextWatcher()
  }

  private fun showError(throwable: Throwable) {
    Snackbreak.bindTo(owner)
        .short(layoutRoot, throwable.message ?: "An unexpected error occurred")
        .show()
  }

  private fun clearError() {
    Snackbreak.bindTo(owner)
        .dismiss()
  }

  override fun onRender(
    state: DetailListViewState,
    oldState: DetailListViewState?
  ) {
    state.throwable.let { throwable ->
      if (throwable == null) {
        clearError()
      } else {
        showError(throwable)
      }
    }

    state.entry.let { entry ->
      updateName(entry.name())
      date.text = SimpleDateFormat.getDateInstance()
          .format(entry.createdTime())
    }

    state.items.let { items ->
      val realItemCount = items
          .asSequence()
          .filter { it.isReal() }
          .filterNot { it.isArchived() }
          .count()

      count.text = "$realItemCount ${if (realItemCount == 1) "Item" else "Items"}"
    }
  }
}
