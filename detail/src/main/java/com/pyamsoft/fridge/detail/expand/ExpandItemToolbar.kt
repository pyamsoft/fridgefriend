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

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.MenuItem
import android.view.ViewGroup
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.databinding.ExpandToolbarBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.ImageTarget
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.util.tintWith
import javax.inject.Inject

class ExpandItemToolbar @Inject internal constructor(
    imageLoader: ImageLoader,
    parent: ViewGroup
) : BaseUiView<ExpandItemViewState, ExpandedItemViewEvent, ExpandToolbarBinding>(parent) {

    override val viewBinding = ExpandToolbarBinding::inflate

    override val layoutRoot by boundView { expandToolbar }

    private var deleteMenuItem: MenuItem? = null
    private var consumeMenuItem: MenuItem? = null
    private var spoilMenuItem: MenuItem? = null
    private var restoreMenuItem: MenuItem? = null

    init {
        doOnInflate {
            imageLoader.load(R.drawable.ic_close_24dp).mutate { it.tintWith(Color.WHITE) }
                .into(object : ImageTarget<Drawable> {

                    override fun clear() {
                        binding.expandToolbar.navigationIcon = null
                    }

                    override fun setImage(image: Drawable) {
                        binding.expandToolbar.setUpEnabled(true, image)
                    }
                }).also { loaded ->
                    doOnTeardown {
                        loaded.dispose()
                    }
                }
        }
        doOnInflate {
            binding.expandToolbar.apply {
                inflateMenu(R.menu.menu_expanded)
                menu.apply {
                    deleteMenuItem = findItem(R.id.menu_item_delete).apply {
                        isVisible = false
                    }
                    consumeMenuItem = findItem(R.id.menu_item_consume).apply {
                        isVisible = false
                    }
                    spoilMenuItem = findItem(R.id.menu_item_spoil).apply {
                        isVisible = false
                    }
                    restoreMenuItem = findItem(R.id.menu_item_restore).apply {
                        isVisible = false
                    }
                }
            }
        }

        doOnInflate {
            binding.expandToolbar.setNavigationOnClickListener(DebouncedOnClickListener.create {
                publish(ExpandedItemViewEvent.CloseItem)
            })

            binding.expandToolbar.setOnMenuItemClickListener { menuItem ->
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
                    R.id.menu_item_restore -> {
                        publish(ExpandedItemViewEvent.RestoreItem)
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
        deleteMenuItem = null
        consumeMenuItem = null
        spoilMenuItem = null
        restoreMenuItem = null

        binding.expandToolbar.menu.clear()
        binding.expandToolbar.setNavigationOnClickListener(null)
        binding.expandToolbar.setOnMenuItemClickListener(null)
    }

    private fun handleItem(state: ExpandItemViewState) {
        state.item.let { item ->
            if (item == null) {
                requireNotNull(deleteMenuItem).isVisible = false
                requireNotNull(consumeMenuItem).isVisible = false
                requireNotNull(spoilMenuItem).isVisible = false
                requireNotNull(restoreMenuItem).isVisible = false
            } else {
                val isReal = item.isReal()
                val isHave = item.presence() == HAVE

                // Always show delete
                requireNotNull(deleteMenuItem).isVisible = isReal

                if (item.isArchived()) {
                    requireNotNull(restoreMenuItem).isVisible = isReal
                    requireNotNull(consumeMenuItem).isVisible = false
                    requireNotNull(spoilMenuItem).isVisible = false
                } else {
                    requireNotNull(consumeMenuItem).isVisible = isReal && isHave
                    requireNotNull(spoilMenuItem).isVisible = isReal && isHave
                    requireNotNull(restoreMenuItem).isVisible = false
                }
            }
        }
    }

    override fun onRender(state: ExpandItemViewState) {
        handleItem(state)
    }
}
