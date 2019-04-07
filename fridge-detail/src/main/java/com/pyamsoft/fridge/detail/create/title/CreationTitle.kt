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

package com.pyamsoft.fridge.detail.create.title

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import com.google.android.material.textfield.TextInputLayout
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.create.title.CreationTitle.Callback
import com.pyamsoft.pydroid.arch.BaseUiView
import javax.inject.Inject

internal class CreationTitle @Inject internal constructor(
  parent: ViewGroup,
  callback: Callback
) : BaseUiView<Callback>(parent, callback) {

  override val layout: Int = R.layout.detail_title

  override val layoutRoot by lazyView<TextInputLayout>(R.id.entry_detail_title)
  private var watcher: TextWatcher? = null

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    layoutRoot.requestFocus()
    addTextWatcher()
  }

  private fun addTextWatcher() {
    requireNotNull(layoutRoot.editText).let {
      watcher = object : TextWatcher {

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
          if (s != null) {
            callback.onUpdateName(s.toString())
          }
        }

      }
      it.addTextChangedListener(watcher)
    }
  }

  private fun removeTextWatcher() {
    watcher?.let { layoutRoot.editText?.removeTextChangedListener(it) }
    watcher = null
  }

  override fun onTeardown() {
    layoutRoot.clearFocus()
    removeTextWatcher()
  }

  fun updateName(name: String, firstUpdate: Boolean) {
    removeTextWatcher()
    val editText = requireNotNull(layoutRoot.editText)
    editText.setTextKeepState(name)
    if (firstUpdate && name.isNotBlank()) {
      editText.setSelection(name.length)
    }
    addTextWatcher()
  }

  fun showError(throwable: Throwable) {
    // TODO
  }

  fun clearError() {
    // TODO
  }

  interface Callback {

    fun onUpdateName(name: String)

  }
}
