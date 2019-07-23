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

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CloseItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.ConsumeItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.DeleteItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.SpoilItem
import com.pyamsoft.fridge.detail.item.DetailItemViewState
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.ImageTarget
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import javax.inject.Inject

class ExpandedToolbar @Inject internal constructor(
  private val imageLoader: ImageLoader,
  parent: ViewGroup
) : BaseUiView<DetailItemViewState, DetailItemViewEvent>(parent) {

  override val layout: Int = R.layout.detail_toolbar

  override val layoutRoot by boundView<Toolbar>(R.id.detail_toolbar)

  private var deleteMenuItem: MenuItem? = null
  private var consumeMenuItem: MenuItem? = null
  private var spoilMenuItem: MenuItem? = null
  private var iconLoaded: Loaded? = null

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    stopIconLoad()
    iconLoaded = imageLoader.load(R.drawable.ic_close_24dp)
        .into(object : ImageTarget<Drawable> {

          override fun clear() {
            layoutRoot.navigationIcon = null
          }

          override fun setError(error: Drawable?) {
            layoutRoot.setUpEnabled(true, error)
          }

          override fun setImage(image: Drawable) {
            layoutRoot.setUpEnabled(true, image)
          }

          override fun setPlaceholder(placeholder: Drawable?) {
            layoutRoot.setUpEnabled(true, placeholder)
          }

          override fun view(): View {
            return layoutRoot
          }

        })

    layoutRoot.inflateMenu(R.menu.menu_expanded)
    deleteMenuItem = layoutRoot.menu.findItem(R.id.menu_item_delete)
    consumeMenuItem = layoutRoot.menu.findItem(R.id.menu_item_consume)
    spoilMenuItem = layoutRoot.menu.findItem(R.id.menu_item_spoil)
  }

  override fun onRender(
    state: DetailItemViewState,
    savedState: UiSavedState
  ) {
    state.item.let { item ->
      layoutRoot.setNavigationOnClickListener(DebouncedOnClickListener.create {
        publish(CloseItem(item))
      })

      requireNotNull(deleteMenuItem).isVisible = item.isReal()
      requireNotNull(consumeMenuItem).isVisible = item.isReal()
      requireNotNull(spoilMenuItem).isVisible = item.isReal()

      layoutRoot.setOnMenuItemClickListener { menuItem ->
        return@setOnMenuItemClickListener when (menuItem.itemId) {
          R.id.menu_item_delete -> {
            publish(DeleteItem(item))
            true
          }
          R.id.menu_item_consume -> {
            publish(ConsumeItem(item))
            true
          }
          R.id.menu_item_spoil -> {
            publish(SpoilItem(item))
            true
          }
          else -> false
        }
      }
    }
  }

  private fun stopIconLoad() {
    iconLoaded?.dispose()
    iconLoaded = null
  }

  override fun onTeardown() {
    stopIconLoad()

    layoutRoot.menu.clear()
    deleteMenuItem = null
    consumeMenuItem = null
    spoilMenuItem = null

    layoutRoot.setNavigationOnClickListener(null)
    layoutRoot.setOnMenuItemClickListener(null)
  }
}
