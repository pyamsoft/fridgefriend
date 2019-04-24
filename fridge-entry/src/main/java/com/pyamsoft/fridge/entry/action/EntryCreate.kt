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

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.content.ContextCompat
import androidx.core.view.ViewPropertyAnimatorListenerAdapter
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pyamsoft.fridge.entry.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.tintWith
import javax.inject.Inject

internal class EntryCreate @Inject internal constructor(
  private val theming: Theming,
  private val imageLoader: ImageLoader,
  parent: ViewGroup,
  callback: EntryCreate.Callback
) : BaseUiView<EntryCreate.Callback>(parent, callback) {

  override val layout: Int = R.layout.entry_create

  override val layoutRoot by boundView<FloatingActionButton>(R.id.entry_create)

  private var createIconLoaded: Loaded? = null

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    layoutRoot.setOnDebouncedClickListener {
      it.wiggle { callback.onCreateClicked() }
    }

    val createBackground: Int
    val iconTint: Int
    if (theming.isDarkTheme()) {
      createBackground = R.color.black
      iconTint = R.color.white
    } else {
      createBackground = R.color.white
      iconTint = R.color.black
    }

    val background = ColorStateList.valueOf(ContextCompat.getColor(view.context, createBackground))
    layoutRoot.supportBackgroundTintList = background

    createIconLoaded?.dispose()
    createIconLoaded = imageLoader.load(R.drawable.ic_add_24dp)
      .mutate { it.tintWith(view.context, iconTint) }
      .into(layoutRoot)
  }

  override fun onTeardown() {
    layoutRoot.setOnClickListener(null)
    createIconLoaded?.dispose()
    createIconLoaded = null
  }

  internal inline fun show(gap: Int, margin: Int, crossinline onShown: () -> Unit) {
    layoutRoot.popShow(listener = object : ViewPropertyAnimatorListenerAdapter() {
      override fun onAnimationStart(view: View?) {
        super.onAnimationStart(view)
        view?.isVisible = true

        // Causes visibility to update?
        layoutRoot.updateLayoutParams<MarginLayoutParams> {
          marginEnd = gap + margin
          bottomMargin = (margin * 1.5).toInt()
        }
      }

      override fun onAnimationEnd(view: View?) {
        super.onAnimationEnd(view)
        onShown()
      }
    })
  }

  interface Callback {

    fun onCreateClicked()

  }

  companion object {
  }

}
