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
import android.widget.CompoundButton
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.detail.R
import javax.inject.Inject
import javax.inject.Named

internal class DetailListItemPresence @Inject internal constructor(
  @Named("detail_entry_id") entryId: String,
  @Named("detail_editable") editable: Boolean,
  item: FridgeItem,
  parent: ViewGroup,
  callback: DetailListItem.Callback
) : DetailListItem(editable, entryId, item, parent, callback) {

  override val layout: Int = R.layout.detail_list_item_presence

  override val layoutRoot by lazyView<CompoundButton>(R.id.detail_item_presence)

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    layoutRoot.isEnabled = item.isReal()
    layoutRoot.isChecked = item.presence() == HAVE

    layoutRoot.setOnCheckedChangeListener { _, isChecked ->
      commit(isChecked)
    }
  }

  override fun onTeardown() {
    layoutRoot.setOnCheckedChangeListener(null)
    layoutRoot.isEnabled = false
    layoutRoot.isChecked = false
  }

  private fun commit(isChecked: Boolean) {
    commitModel(presence = if (isChecked) HAVE else NEED)
  }

}

