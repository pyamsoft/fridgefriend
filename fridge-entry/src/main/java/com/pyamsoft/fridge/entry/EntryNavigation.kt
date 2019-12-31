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

package com.pyamsoft.fridge.entry

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.core.view.updatePadding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pyamsoft.fridge.core.DefaultActivityPage
import com.pyamsoft.fridge.core.DefaultActivityPage.HAVE
import com.pyamsoft.fridge.core.DefaultActivityPage.NEARBY
import com.pyamsoft.fridge.core.DefaultActivityPage.NEED
import com.pyamsoft.fridge.entry.EntryViewEvent.OpenHave
import com.pyamsoft.fridge.entry.EntryViewEvent.OpenNearby
import com.pyamsoft.fridge.entry.EntryViewEvent.OpenNeed
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import timber.log.Timber
import javax.inject.Inject

class EntryNavigation @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<EntryViewState, EntryViewEvent>(parent) {

    override val layout: Int = R.layout.entry_navigation

    override val layoutRoot by boundView<BottomNavigationView>(R.id.entry_bottom_navigation_menu)

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
                    R.id.menu_item_nav_need -> select(OpenNeed)
                    R.id.menu_item_nav_have -> select(OpenHave)
                    R.id.menu_item_nav_nearby -> select(OpenNearby)
                    else -> false
                }
            }
        }

        doOnTeardown {
            layoutRoot.setOnNavigationItemSelectedListener(null)
        }
    }

    override fun onRender(
        state: EntryViewState,
        savedState: UiSavedState
    ) {
        state.page.let { page ->
            val pageId = getIdForPage(page)
            if (pageId != layoutRoot.selectedItemId) {
                layoutRoot.selectedItemId = pageId
                layoutRoot.menu.findItem(pageId).isChecked = true
            }
        }
    }

    @CheckResult
    private fun getIdForPage(page: DefaultActivityPage): Int {
        return when (page) {
            NEED -> R.id.menu_item_nav_need
            HAVE -> R.id.menu_item_nav_have
            NEARBY -> R.id.menu_item_nav_nearby
        }
    }

    @CheckResult
    private fun select(viewEvent: EntryViewEvent): Boolean {
        publish(viewEvent)
        return false
    }
}
