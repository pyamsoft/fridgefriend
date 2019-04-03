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

package com.pyamsoft.fridge.detail.list.item.fridge

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.pydroid.arch.UiToggleView
import javax.inject.Inject
import javax.inject.Named

internal class DetailListItemStrikethrough @Inject internal constructor(
  @Named("detail_entry_id") entryId: String,
  @Named("detail_editable") editable: Boolean,
  item: FridgeItem,
  parent: ViewGroup,
  callback: DetailListItem.Callback
) : DetailListItem(editable, entryId, item, parent, callback), UiToggleView {

  override val layout: Int = R.layout.detail_list_item_strikethrough

  override val layoutRoot by lazyView<ViewGroup>(R.id.detail_item_strikethrough)
  private val strikeThrough by lazyView<View>(R.id.detail_item_strikethrough_line)

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    if (!editable) {
      if (item.presence() == HAVE) {
        show()
      } else {
        hide()
      }
    } else {
      hide()
    }
  }

  override fun onTeardown() {
    hide()
  }

  override fun show() {
    strikeThrough.isVisible = true
  }

  override fun hide() {
    strikeThrough.isInvisible = true
  }

}

