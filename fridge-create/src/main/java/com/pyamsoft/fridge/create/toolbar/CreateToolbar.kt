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

package com.pyamsoft.fridge.create.toolbar

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.pyamsoft.fridge.create.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.ImageTarget
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import javax.inject.Inject

internal class CreateToolbar @Inject internal constructor(
  private val imageLoader: ImageLoader,
  parent: ViewGroup,
  callback: Callback
) : BaseUiView<CreateToolbar.Callback>(parent, callback) {

  override val layout: Int = R.layout.create_toolbar

  override val layoutRoot by lazyView<Toolbar>(R.id.create_toolbar)

  private var toolbarIconLoaded: Loaded? = null

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    toolbarIconLoaded?.dispose()
    toolbarIconLoaded = imageLoader.load(R.drawable.ic_close_24dp)
      .into(object : ImageTarget<Drawable> {

        override fun clear() {
          layoutRoot.setUpEnabled(false)
        }

        override fun setError(error: Drawable?) {
          layoutRoot.setUpEnabled(error != null, error)
        }

        override fun setImage(image: Drawable) {
          layoutRoot.setUpEnabled(true, image)
        }

        override fun setPlaceholder(placeholder: Drawable?) {
          layoutRoot.setUpEnabled(placeholder != null, placeholder)
        }

        override fun view(): View {
          return layoutRoot
        }

      })

    layoutRoot.setNavigationOnClickListener(DebouncedOnClickListener.create {
      callback.onCloseClicked()
    })
  }

  override fun onTeardown() {
    toolbarIconLoaded?.dispose()
    layoutRoot.setNavigationOnClickListener(null)
  }

  interface Callback {

    fun onCloseClicked()
  }

}
