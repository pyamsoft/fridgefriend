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

package com.pyamsoft.fridge.detail.expand

import android.view.MenuItem
import android.view.ViewGroup
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.databinding.ExpandToolbarBinding
import com.pyamsoft.pydroid.arch.BindingUiView
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import javax.inject.Inject

class ExpandItemToolbar @Inject internal constructor(
    parent: ViewGroup
) : BindingUiView<ExpandItemViewState, ExpandedItemViewEvent, ExpandToolbarBinding>(parent) {

    override val viewBinding = ExpandToolbarBinding::inflate

    override val layoutRoot by boundView { expandToolbar }

    private var deleteMenuItem: MenuItem? = null
    private var consumeMenuItem: MenuItem? = null
    private var spoilMenuItem: MenuItem? = null

    init {
        doOnInflate {
            layoutRoot.setUpEnabled(true)
            layoutRoot.inflateMenu(R.menu.menu_expanded)
            deleteMenuItem = layoutRoot.menu.findItem(R.id.menu_item_delete)
            consumeMenuItem = layoutRoot.menu.findItem(R.id.menu_item_consume)
            spoilMenuItem = layoutRoot.menu.findItem(R.id.menu_item_spoil)
        }

        doOnInflate {
            layoutRoot.setNavigationOnClickListener(DebouncedOnClickListener.create {
                publish(ExpandedItemViewEvent.CloseItem)
            })

            layoutRoot.setOnMenuItemClickListener { menuItem ->
                return@setOnMenuItemClickListener when (menuItem.itemId) {
                    R.id.menu_item_delete -> {
                        publish(ExpandedItemViewEvent.DeleteItem)
                        true
                    }
                    R.id.menu_item_consume -> {
                        publish(ExpandedItemViewEvent.ConsumeItem)
                        true
                    }
                    R.id.menu_item_spoil -> {
                        publish(ExpandedItemViewEvent.SpoilItem)
                        true
                    }
                    else -> false
                }
            }
        }

        doOnTeardown {
            clear()
        }
    }

    private fun clear() {
        layoutRoot.menu.clear()
        deleteMenuItem = null
        consumeMenuItem = null
        spoilMenuItem = null

        layoutRoot.setNavigationOnClickListener(null)
        layoutRoot.setOnMenuItemClickListener(null)
    }

    override fun onRender(state: ExpandItemViewState) {
        state.item.let { item ->
            if (item == null) {
                requireNotNull(deleteMenuItem).isVisible = false
                requireNotNull(consumeMenuItem).isVisible = false
                requireNotNull(spoilMenuItem).isVisible = false
            } else {
                requireNotNull(deleteMenuItem).isVisible = item.isReal()
                requireNotNull(consumeMenuItem).isVisible = item.isReal() && item.presence() == HAVE
                requireNotNull(spoilMenuItem).isVisible = item.isReal() && item.presence() == HAVE
            }
        }
    }
}
