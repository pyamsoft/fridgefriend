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

package com.pyamsoft.fridge.detail.item

import android.view.ViewGroup
import android.widget.CompoundButton
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CommitPresence
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import javax.inject.Inject

class DetailListItemPresence @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<DetailItemViewState, DetailItemViewEvent>(parent) {

    override val layout: Int = R.layout.detail_list_item_presence

    override val layoutRoot by boundView<ViewGroup>(R.id.detail_item_presence)

    private val presenceSwitch by boundView<CompoundButton>(R.id.detail_item_presence_switch)

    init {
        doOnTeardown {
            removeListeners()
        }
    }

    override fun onRender(
        state: DetailItemViewState,
        savedState: UiSavedState
    ) {
        state.item.let { item ->
            removeListeners()

            setSwitchEnabled(item)
            presenceSwitch.isChecked = item.presence() == HAVE
            presenceSwitch.setOnCheckedChangeListener { _, isChecked ->
                commit(item, isChecked)
            }
        }
    }

    private fun removeListeners() {
        presenceSwitch.setOnCheckedChangeListener(null)
    }

    private fun setSwitchEnabled(item: FridgeItem) {
        presenceSwitch.isEnabled = !item.isArchived()
    }

    private fun commit(
        item: FridgeItem,
        isChecked: Boolean
    ) {
        publish(CommitPresence(item, if (isChecked) HAVE else NEED))
    }
}
