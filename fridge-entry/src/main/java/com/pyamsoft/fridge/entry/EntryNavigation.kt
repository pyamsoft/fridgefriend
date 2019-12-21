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
import androidx.core.view.isVisible
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
    parent: ViewGroup,
    private val defaultActivityPage: DefaultActivityPage
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
            layoutRoot.isVisible = false
            layoutRoot.setOnNavigationItemSelectedListener(null)
        }

        doOnSaveState { outState ->
            outState.putInt(LAST_PAGE, layoutRoot.selectedItemId)
        }
    }

    override fun onRender(
        state: EntryViewState,
        savedState: UiSavedState
    ) {
        layoutRoot.isVisible = state.entry != null

        // If the navbar was previously not visible and now is, load the default page
        if (layoutRoot.isVisible) {
            selectDefault(savedState)
        }
    }

    @CheckResult
    private fun getIdForDefaultPage(): Int {
        return when (defaultActivityPage) {
            NEED -> R.id.menu_item_nav_need
            HAVE -> R.id.menu_item_nav_have
            NEARBY -> R.id.menu_item_nav_nearby
        }
    }

    private fun selectDefault(savedState: UiSavedState) {
        savedState.consume(LAST_PAGE, PAGE_VALUE_NONE) { itemId ->
            val newSelectedItem = if (itemId != PAGE_VALUE_NONE) itemId else {
                val id = getIdForDefaultPage()
                val item = layoutRoot.menu.findItem(id)
                item?.itemId ?: PAGE_VALUE_NONE
            }

            if (newSelectedItem != PAGE_VALUE_NONE) {
                layoutRoot.selectedItemId = newSelectedItem
            }
        }
    }

    @CheckResult
    private fun select(viewEvent: EntryViewEvent): Boolean {
        publish(viewEvent)
        return true
    }

    companion object {

        private const val PAGE_VALUE_NONE = 0
        private const val LAST_PAGE = "last_page"
    }
}
