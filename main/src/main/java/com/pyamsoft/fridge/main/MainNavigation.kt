/*
 * Copyright 2020 Peter Kenji Yamanaka
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

import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import com.pyamsoft.fridge.main.databinding.MainNavigationBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import timber.log.Timber
import javax.inject.Inject
import com.pyamsoft.fridge.ui.R as R2

class MainNavigation @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<MainViewState, MainViewEvent, MainNavigationBinding>(parent) {

    override val viewBinding = MainNavigationBinding::inflate

    override val layoutRoot by boundView { mainBottomNavigationMenu }

    private val handler = Handler(Looper.getMainLooper())

    init {
        doOnInflate {
            correctBackground()
        }

        doOnInflate {
            layoutRoot.doOnApplyWindowInsets { view, insets, _ ->
                view.updatePadding(
                    bottom = insets.systemWindowInsetBottom,
                    left = 0,
                    right = 0,
                    top = 0
                )

                // Make sure we are laid out before grabbing the height
                view.doOnLayout { v ->
                    // Publish the measured height
                    publish(MainViewEvent.BottomBarMeasured(v.height))
                }
            }
        }

        doOnInflate {
            binding.mainBottomNavigationMenu.setOnNavigationItemSelectedListener { item ->
                Timber.d("Click nav item: $item")
                return@setOnNavigationItemSelectedListener when (item.itemId) {
                    R.id.menu_item_nav_entries -> select(MainViewEvent.OpenEntries)
                    R.id.menu_item_nav_category -> select(MainViewEvent.OpenCategory)
                    R.id.menu_item_nav_nearby -> select(MainViewEvent.OpenNearby)
                    R.id.menu_item_nav_settings -> select(MainViewEvent.OpenSettings)
                    else -> false
                }
            }
        }

        doOnTeardown {
            binding.mainBottomNavigationMenu.setOnNavigationItemSelectedListener(null)
            binding.mainBottomNavigationMenu.removeBadge(R.id.menu_item_nav_entries)
        }

        doOnTeardown {
            handler.removeCallbacksAndMessages(null)
        }
    }

    /**
     * Default MaterialShapeBackground makes a weird shadow thing, disable it since it looks
     * funny through the transparent bar
     */
    private fun correctBackground() {
        val color = ContextCompat.getColor(layoutRoot.context, R2.color.colorPrimarySeeThrough)
        binding.mainBottomNavigationMenu.setBackgroundColor(color)
    }

    private fun handlePage(state: MainViewState) {
        state.page.let { page ->
            val pageId = getIdForPage(page)
            if (pageId != 0) {
                // Don't mark it selected since this will re-fire the click event
                // binding.mainBottomNavigationMenu.selectedItemId = pageId
                val item = binding.mainBottomNavigationMenu.menu.findItem(pageId)
                if (item != null) {
                    handler.removeCallbacksAndMessages(null)
                    handler.post { item.isChecked = true }
                }
            }
        }
    }

    override fun onRender(state: MainViewState) {
        correctBackground()
        handlePage(state)
    }

    @CheckResult
    private fun getIdForPage(page: MainPage?): Int {
        return if (page == null) 0 else {
            when (page) {
                MainPage.ENTRIES -> R.id.menu_item_nav_entries
                MainPage.CATEGORY -> R.id.menu_item_nav_category
                MainPage.NEARBY -> R.id.menu_item_nav_nearby
                MainPage.SETTINGS -> R.id.menu_item_nav_settings
            }
        }
    }

    @CheckResult
    private fun select(viewEvent: MainViewEvent): Boolean {
        publish(viewEvent)
        return false
    }
}
