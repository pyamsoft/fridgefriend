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
import android.view.animation.Animation
import androidx.core.view.ViewPropertyAnimatorListenerAdapter
import androidx.core.view.isVisible
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pyamsoft.fridge.entry.R
import com.pyamsoft.fridge.entry.action.EntryActionViewEvent.CreateClicked
import com.pyamsoft.fridge.entry.action.EntryActionViewEvent.ShowCreate
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.tintWith
import javax.inject.Inject

class EntryCreate @Inject internal constructor(
  private val theming: Theming,
  private val imageLoader: ImageLoader,
  parent: ViewGroup
) : BaseUiView<EntryActionViewState, EntryActionViewEvent>(parent) {

  override val layout: Int = R.layout.entry_create

  override val layoutRoot by boundView<FloatingActionButton>(R.id.entry_create)

  private var createIconLoaded: Loaded? = null
  private var animation: Animation? = null

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    layoutRoot.setOnDebouncedClickListener {
      animation?.cancel()
      animation = it.wiggle { publish(CreateClicked) }
    }

    createIconLoaded?.dispose()
    createIconLoaded = imageLoader.load(R.drawable.ic_add_24dp)
        .mutate { it.tintWith(view.context, R.color.white) }
        .into(layoutRoot)
  }

  override fun onTeardown() {
    layoutRoot.setOnClickListener(null)
    createIconLoaded?.dispose()
    animation?.cancel()

    createIconLoaded = null
    animation = null
  }

  override fun onRender(
    state: EntryActionViewState,
    oldState: EntryActionViewState?
  ) {
    state.isShown.let { shown ->
      if (!shown) {
        show()
        publish(ShowCreate)
      }
    }
  }

  private fun show() {
    layoutRoot.popShow(listener = object : ViewPropertyAnimatorListenerAdapter() {
      override fun onAnimationStart(view: View?) {
        super.onAnimationStart(view)
        if (view != null) {
          view.isVisible = true
        }
      }
    })
  }

}
