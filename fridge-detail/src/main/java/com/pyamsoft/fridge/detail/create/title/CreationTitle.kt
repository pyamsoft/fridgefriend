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
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.textfield.TextInputLayout
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.create.title.CreationTitleViewEvent.NameUpdate
import com.pyamsoft.pydroid.arch.impl.BaseUiView
import com.pyamsoft.pydroid.arch.impl.onChange
import com.pyamsoft.pydroid.ui.util.Snackbreak
import javax.inject.Inject

class CreationTitle @Inject internal constructor(
  private val owner: LifecycleOwner,
  parent: ViewGroup
) : BaseUiView<CreationTitleViewState, CreationTitleViewEvent>(parent) {

  override val layout: Int = R.layout.detail_title

  override val layoutRoot by boundView<TextInputLayout>(R.id.entry_detail_title)
  private var watcher: TextWatcher? = null

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    layoutRoot.requestFocus()
    addTextWatcher()
  }

  private fun addTextWatcher() {
    requireNotNull(layoutRoot.editText).let {
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
    watcher?.let { layoutRoot.editText?.removeTextChangedListener(it) }
    watcher = null
  }

  override fun onTeardown() {
    removeTextWatcher()
    clearError()
    layoutRoot.clearFocus()
    layoutRoot.editText?.text?.clear()
    layoutRoot.editText?.clearFocus()
  }

  private fun updateName(name: String) {
    removeTextWatcher()
    val editText = requireNotNull(layoutRoot.editText)
    editText.setTextKeepState(name)
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
    state: CreationTitleViewState,
    oldState: CreationTitleViewState?
  ) {
    state.onChange(oldState, field = { it.name }) { name ->
      updateName(name)
    }

    state.onChange(oldState, field = { it.throwable }) { throwable ->
      if (throwable == null) {
        clearError()
      } else {
        showError(throwable)
      }
    }
  }
}
