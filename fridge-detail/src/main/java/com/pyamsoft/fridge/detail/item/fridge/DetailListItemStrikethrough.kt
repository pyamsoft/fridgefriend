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

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.fridge.DetailItemViewEvent.ExpandItem
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class DetailListItemStrikethrough @Inject internal constructor(
  parent: ViewGroup
) : BaseUiView<DetailItemViewState, DetailItemViewEvent>(parent) {

  override val layout: Int = R.layout.detail_list_item_strikethrough

  override val layoutRoot by boundView<ViewGroup>(R.id.detail_item_strikethrough)

  private val strikeLine by boundView<View>(R.id.detail_item_strikethrough_line)
  private val errorMessage by boundView<TextView>(R.id.detail_item_strikethrough_error)

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
  }

  override fun onRender(
    state: DetailItemViewState,
    oldState: DetailItemViewState?
  ) {
    state.item.let { item ->
      removeListeners()
      val isEditable = state.isEditable
      decideStrikethroughState(item, isEditable)
      layoutRoot.setOnDebouncedClickListener {
        if (isEditable) {
          publish(ExpandItem(item))
        }
      }
    }

    state.throwable.let { throwable ->
      if (throwable == null) {
        clearError()
      } else {
        showError(throwable)
      }
    }
  }

  private fun decideStrikethroughState(
    item: FridgeItem,
    isEditable: Boolean
  ) {
    if (isEditable && !item.isArchived()) {
      hide()
    } else {
      if (item.isArchived()) {
        show()
      } else {
        if (item.presence() == HAVE) {
          show()
        } else {
          hide()
        }
      }
    }
  }

  override fun onTeardown() {
    removeListeners()
    clearError()
  }

  private fun removeListeners() {
    layoutRoot.setOnDebouncedClickListener(null)
  }

  private fun show() {
    strikeLine.isVisible = true
  }

  private fun hide() {
    strikeLine.isInvisible = true
  }

  private fun showError(throwable: Throwable) {
    val typeStyle: Int
    if (throwable is IllegalArgumentException) {
      typeStyle = Typeface.BOLD
    } else {
      typeStyle = Typeface.NORMAL
    }
    errorMessage.setTypeface(errorMessage.typeface, typeStyle)
    errorMessage.text = throwable.message ?: "ERROR: Unknown error, please try again later."
  }

  private fun clearError() {
    errorMessage.text = ""
  }
}

