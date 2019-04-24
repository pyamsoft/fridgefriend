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

package com.pyamsoft.fridge.entry.action

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginEnd
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pyamsoft.fridge.entry.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

internal class EntryShop @Inject internal constructor(
  private val imageLoader: ImageLoader,
  parent: ViewGroup,
  callback: EntryShop.Callback
) : BaseUiView<EntryShop.Callback>(parent, callback) {

  override val layout: Int = R.layout.entry_shop

  override val layoutRoot by boundView<FloatingActionButton>(R.id.entry_shop)

  private var shopIconLoaded: Loaded? = null

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    layoutRoot.setOnDebouncedClickListener {
      it.wiggle { callback.onShopClicked() }
    }

    shopIconLoaded?.dispose()
    shopIconLoaded = imageLoader.load(R.drawable.ic_shopping_cart_24dp)
      .into(layoutRoot)
  }

  override fun onTeardown() {
    layoutRoot.setOnClickListener(null)
    shopIconLoaded?.dispose()
    shopIconLoaded = null
  }

  internal inline fun onLaidOut(crossinline onLaidOut: (gap: Int, margin: Int) -> Unit) {
    layoutRoot.post {
      val width = layoutRoot.width
      val margin = layoutRoot.marginEnd
      onLaidOut(width + margin, margin)
    }
  }

  internal fun show() {
    layoutRoot.popShow()
  }

  interface Callback {

    fun onShopClicked()

  }

}
