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
import com.pyamsoft.pydroid.arch.UiToggleView
import javax.inject.Inject
import javax.inject.Named

internal class DetailListItemStrikethrough @Inject internal constructor(
  @Named("item_editable") private val editable: Boolean,
  item: FridgeItem,
  parent: ViewGroup,
  callback: Callback
) : DetailListItem<DetailListItemStrikethrough.Callback>(item, parent, callback),
  UiToggleView {

  override val layout: Int = R.layout.detail_list_item_strikethrough

  override val layoutRoot by boundView<ViewGroup>(R.id.detail_item_strikethrough)
  private val strikeLine by boundView<View>(R.id.detail_item_strikethrough_line)
  private val errorMessage by boundView<TextView>(R.id.detail_item_strikethrough_error)

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    decideStrikethroughState()
  }

  private fun decideStrikethroughState() {
    if (editable && !item.isArchived()) {
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
    hide()
  }

  override fun show() {
    strikeLine.isVisible = true
  }

  override fun hide() {
    strikeLine.isInvisible = true
  }

  override fun onItemUpdated() {
    decideStrikethroughState()
  }

  fun showError(throwable: Throwable) {
    val typeStyle: Int
    if (throwable is IllegalArgumentException) {
      typeStyle = Typeface.BOLD
    } else {
      typeStyle = Typeface.NORMAL
    }
    errorMessage.setTypeface(errorMessage.typeface, typeStyle)
    errorMessage.text = throwable.message ?: "ERROR: Unknown error, please try again later."
  }

  fun clearError() {
    errorMessage.text = ""
  }

  interface Callback : DetailListItem.Callback
}

