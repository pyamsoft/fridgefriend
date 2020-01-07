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
 *
 */

package com.pyamsoft.fridge.main

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.core.view.updatePadding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import javax.inject.Inject
import timber.log.Timber

class MainNavigation @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<MainViewState, MainViewEvent>(parent) {

    override val layout: Int = R.layout.main_navigation

    override val layoutRoot by boundView<BottomNavigationView>(R.id.main_bottom_navigation_menu)

    init {
        doOnInflate {
            layoutRoot.doOnApplyWindowInsets { v, insets, padding ->
                v.updatePadding(bottom = padding.bottom + insets.systemWindowInsetBottom)
            }
        }

        doOnInflate {
            layoutRoot.setOnNavigationItemSelectedListener { item ->
                Timber.d("Click nav item: $item")
                return@setOnNavigationItemSelectedListener when (item.itemId) {
                    R.id.menu_item_nav_need -> select(MainViewEvent.OpenNeed)
                    R.id.menu_item_nav_have -> select(MainViewEvent.OpenHave)
                    R.id.menu_item_nav_nearby -> select(MainViewEvent.OpenNearby)
                    else -> false
                }
            }
        }

        doOnTeardown {
            layoutRoot.setOnNavigationItemSelectedListener(null)
        }
    }

    override fun onRender(
        state: MainViewState,
        savedState: UiSavedState
    ) {
        state.page.let { page ->
            val pageId = getIdForPage(page)
            if (pageId != 0) {
                layoutRoot.selectedItemId = pageId
                layoutRoot.menu.findItem(pageId).isChecked = true
            }
        }
    }

    @CheckResult
    private fun getIdForPage(page: MainPage?): Int {
        return if (page == null) 0 else {
            when (page) {
                MainPage.NEED -> R.id.menu_item_nav_need
                MainPage.HAVE -> R.id.menu_item_nav_have
                MainPage.NEARBY -> R.id.menu_item_nav_nearby
            }
        }
    }

    @CheckResult
    private fun select(viewEvent: MainViewEvent): Boolean {
        publish(viewEvent)
        return false
    }
}
