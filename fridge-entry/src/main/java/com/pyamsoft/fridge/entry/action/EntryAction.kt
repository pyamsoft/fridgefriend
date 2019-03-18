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
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pyamsoft.fridge.entry.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiToggleView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.popHide
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.tintWith
import javax.inject.Inject

internal class EntryAction @Inject internal constructor(
  private val theming: Theming,
  private val imageLoader: ImageLoader,
  parent: ViewGroup,
  callback: EntryAction.Callback
) : BaseUiView<EntryAction.Callback>(parent, callback), UiToggleView {

  override val layout: Int = R.layout.entry_action

  override val layoutRoot by lazyView<ViewGroup>(R.id.entry_action_container)
  private val createButton by lazyView<FloatingActionButton>(R.id.entry_action_create)
  private val shopButton by lazyView<FloatingActionButton>(R.id.entry_action_shop)

  private var createIconLoaded: Loaded? = null
  private var shopIconLoaded: Loaded? = null

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    createButton.setOnDebouncedClickListener {
      it.wiggle { callback.onCreateClicked() }
    }

    shopButton.setOnDebouncedClickListener {
      it.wiggle { callback.onShopClicked() }
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
    createButton.supportBackgroundTintList = background

    createIconLoaded?.dispose()
    shopIconLoaded?.dispose()
    createIconLoaded = imageLoader.load(R.drawable.ic_add_24dp)
      .mutate { it.tintWith(view.context, iconTint) }
      .into(createButton)

    shopIconLoaded = imageLoader.load(R.drawable.ic_shopping_cart_24dp)
      .into(shopButton)
  }

  override fun onTeardown() {
    createButton.setOnClickListener(null)
    shopButton.setOnClickListener(null)

    createIconLoaded?.dispose()
    shopIconLoaded?.dispose()
  }

  override fun show() {
    createButton.popShow(FIRST_DELAY_TIME * 2)
    shopButton.popShow(STAGGERED_DELAY_TIME * 2)
  }

  override fun hide() {
    shopButton.popHide(FIRST_DELAY_TIME)
    createButton.popHide(STAGGERED_DELAY_TIME)
  }

  private inline fun View.wiggle(crossinline onAnimationComplete: () -> Unit) {
    val animation = AnimationUtils.loadAnimation(context, R.anim.wiggle).apply {
      setAnimationListener(object : AnimationListener {

        override fun onAnimationRepeat(animation: Animation?) {
        }

        override fun onAnimationEnd(animation: Animation?) {
          onAnimationComplete()
          setAnimationListener(null)
        }

        override fun onAnimationStart(animation: Animation?) {
        }

      })
    }
    startAnimation(animation)
  }

  interface Callback {

    fun onCreateClicked()

    fun onShopClicked()

  }

  companion object {

    private const val FIRST_DELAY_TIME = 100L
    private const val STAGGERED_DELAY_TIME = 300L
  }

}
