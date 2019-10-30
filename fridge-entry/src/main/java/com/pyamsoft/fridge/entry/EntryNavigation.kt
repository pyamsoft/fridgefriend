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
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.EntryViewEvent.OpenHave
import com.pyamsoft.fridge.entry.EntryViewEvent.OpenNearby
import com.pyamsoft.fridge.entry.EntryViewEvent.OpenNeed
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import javax.inject.Inject

class EntryNavigation @Inject internal constructor(
    private val defaultPage: DefaultActivityPage?,
    parent: ViewGroup
) : BaseUiView<EntryViewState, EntryViewEvent>(parent) {

    override val layout: Int = R.layout.entry_navigation

    override val layoutRoot by boundView<BottomNavigationView>(R.id.entry_bottom_navigation_menu)

    init {
        doOnInflate {
            layoutRoot.isVisible = false

            layoutRoot.doOnApplyWindowInsets { v, insets, padding ->
                v.updatePadding(bottom = padding.bottom + insets.systemWindowInsetBottom)
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

        layoutRoot.setOnNavigationItemSelectedListener { item ->
            return@setOnNavigationItemSelectedListener when (item.itemId) {
                R.id.menu_item_nav_need -> handleClick(state.entry) { OpenNeed(it) }
                R.id.menu_item_nav_have -> handleClick(state.entry) { OpenHave(it) }
                R.id.menu_item_nav_nearby -> handleClick(state.entry) { OpenNearby(it) }
                else -> false
            }
        }

        selectDefault(state.entry, savedState)
    }

    @CheckResult
    private fun getIdForPage(page: DefaultActivityPage): Int {
        return when (page) {
            NEED -> R.id.menu_item_nav_need
            HAVE -> R.id.menu_item_nav_have
            NEARBY -> R.id.menu_item_nav_nearby
        }
    }

    private fun selectDefault(
        entry: FridgeEntry?,
        savedState: UiSavedState
    ) {
        if (entry != null) {
            savedState.consume(LAST_PAGE, layoutRoot.selectedItemId) { itemId ->
                layoutRoot.selectedItemId = if (itemId != 0) itemId else {
                    defaultPage.let { page ->
                        if (page == null) {
                            itemId
                        } else {
                            val id = getIdForPage(page)
                            val item = layoutRoot.menu.findItem(id)
                            item?.itemId ?: itemId
                        }
                    }
                }
            }
        }
    }

    @CheckResult
    private fun handleClick(
        entry: FridgeEntry?,
        getEvent: (entry: FridgeEntry) -> EntryViewEvent
    ): Boolean {
        return if (entry != null) {
            publish(getEvent(entry))
            true
        } else {
            false
        }
    }

    companion object {

        private const val LAST_PAGE = "last_page"
    }
}
