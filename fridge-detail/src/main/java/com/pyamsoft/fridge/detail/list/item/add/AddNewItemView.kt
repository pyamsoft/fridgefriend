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

package com.pyamsoft.fridge.detail.list.item.add

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.pydroid.arch.UiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.arch.InvalidIdException
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.tintWith
import kotlin.LazyThreadSafetyMode.NONE

internal class AddNewItemView internal constructor(
  private val parent: View,
  private val theming: Theming,
  private val imageLoader: ImageLoader,
  private val callback: Callback
) : UiView {

  private val addNewIcon by lazy(NONE) {
    parent.findViewById<ImageView>(R.id.detail_add_new_item_icon)
  }
  private var iconLoaded: Loaded? = null

  override fun id(): Int {
    throw InvalidIdException
  }

  override fun inflate(savedInstanceState: Bundle?) {
    disposeIcon()
    iconLoaded = imageLoader.load(R.drawable.ic_add_24dp)
      .mutate { drawable ->
        val color: Int
        if (theming.isDarkTheme()) {
          color = R.color.white
        } else {
          color = R.color.black
        }
        return@mutate drawable.tintWith(parent.context, color)
      }.into(addNewIcon)

    parent.setOnDebouncedClickListener { callback.onAddNewClicked() }
  }

  override fun saveState(outState: Bundle) {
  }

  override fun teardown() {
    disposeIcon()
    addNewIcon.setImageDrawable(null)
    parent.setOnClickListener(null)
  }

  private fun disposeIcon() {
    iconLoaded?.dispose()
    iconLoaded = null
  }

  interface Callback {

    fun onAddNewClicked()
  }
}

