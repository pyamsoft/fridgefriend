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
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.detail.R
import javax.inject.Inject

internal class DetailListItemPresence @Inject internal constructor(
  item: FridgeItem,
  parent: ViewGroup,
  callback: DetailListItemPresence.Callback
) : DetailListItem<DetailListItemPresence.Callback>(item, parent, callback) {

  override val layout: Int = R.layout.detail_list_item_presence

  override val layoutRoot by lazyView<ViewGroup>(R.id.detail_item_presence)
  private val presenceSwitch by lazyView<CompoundButton>(R.id.detail_item_presence_switch)

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    presenceSwitch.isEnabled = item.isReal()
    presenceSwitch.isChecked = item.presence() == HAVE
    presenceSwitch.setOnCheckedChangeListener { _, isChecked ->
      commit(isChecked)
    }
  }

  override fun onTeardown() {
    presenceSwitch.setOnCheckedChangeListener(null)
    presenceSwitch.isEnabled = false
    presenceSwitch.isChecked = false
  }

  private fun commit(isChecked: Boolean) {
    callback.commitPresence(item, if (isChecked) HAVE else NEED)
  }

  fun enable() {
    markReal()
    presenceSwitch.isEnabled = item.isReal()
  }

  interface Callback : DetailListItem.Callback {

    fun commitPresence(oldItem: FridgeItem, presence: Presence)

  }

}

