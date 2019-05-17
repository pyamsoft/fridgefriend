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

package com.pyamsoft.fridge.detail.item.add

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.add.AddNewViewEvent.AddNewItemEvent
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UnitViewState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.tintWith
import javax.inject.Inject

class AddNewItemView @Inject internal constructor(
  private val theming: Theming,
  private val imageLoader: ImageLoader,
  parent: ViewGroup
) : BaseUiView<UnitViewState, AddNewViewEvent>(parent) {

  private val addNewIcon by boundView<ImageView>(R.id.detail_add_new_item_icon)

  private var iconLoaded: Loaded? = null

  override val layout: Int = R.layout.add_new_list_item

  override val layoutRoot by boundView<ViewGroup>(R.id.detail_add_new_item)

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    iconLoaded = imageLoader.load(R.drawable.ic_add_24dp)
        .mutate { drawable ->
          val color: Int
          if (theming.isDarkTheme()) {
            color = R.color.white
          } else {
            color = R.color.black
          }
          return@mutate drawable.tintWith(layoutRoot.context, color)
        }
        .into(addNewIcon)

    layoutRoot.setOnDebouncedClickListener { publish(AddNewItemEvent) }
  }

  override fun onRender(
    state: UnitViewState,
    oldState: UnitViewState?
  ) {
  }

  override fun onTeardown() {
    disposeIcon()
    addNewIcon.setImageDrawable(null)
    layoutRoot.setOnClickListener(null)
  }

  private fun disposeIcon() {
    iconLoaded?.dispose()
    iconLoaded = null
  }
}

