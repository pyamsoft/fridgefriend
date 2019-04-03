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
import android.widget.ImageView
import androidx.core.view.isVisible
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.tintWith
import javax.inject.Inject

internal class DetailListItemDelete @Inject internal constructor(
  item: FridgeItem,
  private val theming: Theming,
  private val imageLoader: ImageLoader,
  parent: ViewGroup,
  callback: DetailListItemDelete.Callback
) : DetailListItem<DetailListItemDelete.Callback>(item, parent, callback) {

  private var deleteIconLoaded: Loaded? = null

  override val layout: Int = R.layout.detail_list_item_delete

  override val layoutRoot by lazyView<ImageView>(R.id.detail_item_delete)

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    setupDelete()
  }

  override fun onTeardown() {
    layoutRoot.setOnClickListener(null)
    layoutRoot.setImageDrawable(null)
    deleteIconLoaded?.dispose()
    deleteIconLoaded = null
  }

  private fun setupDelete() {
    deleteIconLoaded?.dispose()
    deleteIconLoaded = null

    layoutRoot.isVisible = true
    deleteIconLoaded = imageLoader.load(R.drawable.ic_close_24dp)
      .mutate { drawable ->
        val color: Int
        if (theming.isDarkTheme()) {
          color = R.color.white
        } else {
          color = R.color.black
        }
        return@mutate drawable.tintWith(layoutRoot.context, color)
      }.into(layoutRoot)

    layoutRoot.setOnDebouncedClickListener {
      layoutRoot.setOnClickListener(null)
      callback.onDelete(item)
    }
  }

  interface Callback {

    fun onDelete(item: FridgeItem)

  }
}

