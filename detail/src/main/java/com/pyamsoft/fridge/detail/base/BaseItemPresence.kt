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
import com.pyamsoft.fridge.detail.databinding.DetailListItemPresenceBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

abstract class BaseItemPresence<S : UiViewState, V : UiViewEvent> protected constructor(
    parent: ViewGroup
) : BaseUiView<S, V, DetailListItemPresenceBinding>(parent) {

    final override val viewBinding = DetailListItemPresenceBinding::inflate

    final override val layoutRoot by boundView { detailItemPresence }

    private val listener = CompoundButton.OnCheckedChangeListener { _, _ ->
        publishChangePresence()
    }

    protected fun renderItem(item: FridgeItem?) {
        binding.detailItemPresenceSwitch.setOnCheckedChangeListener(null)
        binding.detailItemPresenceSwitch.isEnabled = item != null && !item.isArchived()
        if (item != null) {
            binding.detailItemPresenceSwitch.isVisible = true
            binding.detailItemPresenceSwitch.isChecked = item.presence() == FridgeItem.Presence.HAVE
            binding.detailItemPresenceSwitch.setOnCheckedChangeListener(listener)
        } else {
            binding.detailItemPresenceSwitch.isInvisible = true
        }
    }

    protected abstract fun publishChangePresence()
}
