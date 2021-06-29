/*
 * Copyright 2021 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.fridge.main

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.main.databinding.MainNavigationBinding
import com.pyamsoft.fridge.ui.animatePopInFromBottom
import com.pyamsoft.fridge.ui.createRoundedBackground
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.util.asDp
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import javax.inject.Inject
import timber.log.Timber

class MainNavigation
@Inject
internal constructor(
    owner: LifecycleOwner,
    parent: ViewGroup,
) : BaseUiView<MainViewState, MainViewEvent, MainNavigationBinding>(parent) {

  override val viewBinding = MainNavigationBinding::inflate

  override val layoutRoot by boundView { mainBottomBar }

  private var backgroundDrawable: Drawable? = null

  private val handler = Handler(Looper.getMainLooper())
  private var animator: ViewPropertyAnimatorCompat? = null

  init {
    doOnInflate {
      backgroundDrawable = createRoundedBackground(layoutRoot.context)
      correctBackground()
      animateIn()
    }

    doOnInflate {
      layoutRoot.doOnApplyWindowInsets(owner) { view, insets, _ ->
        // Set padding to all zero otherwise the bottom bar gets too puffy when nav buttons are
        // enabled
        view.updatePadding(left = 0, right = 0, top = 0, bottom = 0)

        // Make sure we are laid out before grabbing the height
        view.post {
          // Publish the measured height
          publish(MainViewEvent.BottomBarMeasured(view.height))
        }
      }
    }

    doOnInflate {
      binding.mainBottomNavigationMenu.setOnNavigationItemSelectedListener { item ->
        Timber.d("Click nav item: $item")
        return@setOnNavigationItemSelectedListener when (item.itemId) {
          R.id.menu_item_nav_entries -> select(MainViewEvent.OpenEntries)
          R.id.menu_item_nav_category -> select(MainViewEvent.OpenCategory)
          R.id.menu_item_nav_settings -> select(MainViewEvent.OpenSettings)
          R.id.menu_item_nav_search -> select(MainViewEvent.OpenSearch)
          else -> false
        }
      }
    }

    doOnTeardown {
      binding.mainBottomNavigationMenu.setOnNavigationItemSelectedListener(null)
      binding.mainBottomNavigationMenu.removeBadge(R.id.menu_item_nav_entries)
    }

    doOnTeardown { handler.removeCallbacksAndMessages(null) }

    doOnTeardown {
      animator?.cancel()
      animator = null

      backgroundDrawable = null
    }
  }

  /**
   * Default MaterialShapeBackground makes a weird shadow thing, disable it since it looks funny
   * through the transparent bar
   */
  private fun correctBackground() {
    layoutRoot.apply {
      background = requireNotNull(backgroundDrawable)
      elevation = 8.asDp(context).toFloat()
    }
  }

  private fun animateIn() {
    if (animator == null) {
      animator = animatePopInFromBottom(layoutRoot, 600, false)
    }
  }

  override fun onRender(state: UiRender<MainViewState>) {
    correctBackground()
    state.mapChanged { it.page }.render(viewScope) { handlePage(it) }
  }

  private fun handlePage(page: MainPage?) {
    Timber.d("Handle page: $page")
    val pageId = getIdForPage(page)
    if (pageId != 0) {
      Timber.d("Mark page selected: $page $pageId")
      // Don't mark it selected since this will re-fire the click event
      // binding.mainBottomNavigationMenu.selectedItemId = pageId
      val item = binding.mainBottomNavigationMenu.menu.findItem(pageId)
      if (item != null) {
        handler.removeCallbacksAndMessages(null)
        handler.post { item.isChecked = true }
      }
    }
  }

  @CheckResult
  private fun select(viewEvent: MainViewEvent): Boolean {
    publish(viewEvent)
    return false
  }

  companion object {

    @JvmStatic
    @CheckResult
    private fun getIdForPage(page: MainPage?): Int {
      return if (page == null) 0
      else {
        when (page) {
          is MainPage.Entries -> R.id.menu_item_nav_entries
          is MainPage.Category -> R.id.menu_item_nav_category
          is MainPage.Settings -> R.id.menu_item_nav_settings
          is MainPage.Search -> R.id.menu_item_nav_search
        }
      }
    }
  }
}
