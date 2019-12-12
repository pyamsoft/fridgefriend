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

package com.pyamsoft.fridge.detail.base

import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

abstract class BaseItemPresence<S : UiViewState, V : UiViewEvent> protected constructor(
    parent: ViewGroup
) : BaseUiView<S, V>(parent) {

    final override val layout: Int = R.layout.detail_list_item_presence

    final override val layoutRoot by boundView<ViewGroup>(R.id.detail_item_presence)

    private val presenceSwitch by boundView<CompoundButton>(R.id.detail_item_presence_switch)

    init {
        doOnTeardown {
            removeListeners()
        }
    }

    protected fun render(item: FridgeItem?) {
        removeListeners()

        setSwitchEnabled(item)
        if (item != null) {
            presenceSwitch.isVisible = true
            presenceSwitch.isChecked = item.presence() == FridgeItem.Presence.HAVE
            presenceSwitch.setOnCheckedChangeListener { _, isChecked ->
                commit(item, if (isChecked) FridgeItem.Presence.HAVE else FridgeItem.Presence.NEED)
            }
        } else {
            presenceSwitch.isInvisible = true
        }
    }

    private fun removeListeners() {
        presenceSwitch.setOnCheckedChangeListener(null)
    }

    private fun setSwitchEnabled(item: FridgeItem?) {
        presenceSwitch.isEnabled = item != null && !item.isArchived()
    }

    protected abstract fun commit(item: FridgeItem, presence: FridgeItem.Presence)
}
