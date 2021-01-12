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

package com.pyamsoft.fridge.detail

import android.view.View
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.ui.view.UiToolbar
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import javax.inject.Inject

class DetailToolbar @Inject internal constructor(
    toolbarActivity: ToolbarActivity,
) : UiToolbar<DetailViewState.Sorts, DetailViewState, DetailViewEvent.ControlledEvents.ToolbarEvent>(
    withToolbar = { toolbarActivity.withToolbar(it) }
) {

    private val itemIdPurchasedDate = View.generateViewId()
    private val itemIdExpirationDate = View.generateViewId()

    init {
        doOnInflate {
            toolbarActivity.withToolbar { toolbar ->
                toolbar.setUpEnabled(true)
                toolbar.setNavigationOnClickListener(DebouncedOnClickListener.create {
                    publish(DetailViewEvent.ControlledEvents.ToolbarEvent.Back)
                })
            }
        }

        doOnTeardown {
            toolbarActivity.withToolbar { toolbar ->
                toolbar.setUpEnabled(false)
                toolbar.setNavigationOnClickListener(null)
            }
        }
    }

    override fun publishSearchEvent(search: String) {
        publish(DetailViewEvent.ControlledEvents.ToolbarEvent.SearchQuery(search))
    }

    override fun publishSortEvent(sort: State.Sort<DetailViewState.Sorts>) {
        publish(DetailViewEvent.ControlledEvents.ToolbarEvent.ChangeSort(sort.original))
    }

    override fun onGetSortForMenuItem(itemId: Int): DetailViewState.Sorts? {
        return when (itemId) {
            itemIdCreatedDate -> DetailViewState.Sorts.CREATED
            itemIdName -> DetailViewState.Sorts.NAME
            itemIdPurchasedDate -> DetailViewState.Sorts.PURCHASED
            itemIdExpirationDate -> DetailViewState.Sorts.EXPIRATION
            else -> null
        }
    }

    override fun onCreateAdditionalSortItems(adder: (Int, CharSequence) -> Unit) {
        adder(itemIdPurchasedDate, "Purchase Date")
        adder(itemIdExpirationDate, "Expiration Date")
    }

    override fun onRender(state: UiRender<DetailViewState>) {
        state.distinctBy { it.listItemPresence }.render(viewScope) { handleExtraSubItems(it) }
    }

    private fun handleExtraSubItems(presence: FridgeItem.Presence) {
        val isHavePresence = presence == FridgeItem.Presence.HAVE
        val showExtraMenuItems = isHavePresence && !isSearchExpanded()
        setItemVisibility(itemIdPurchasedDate, showExtraMenuItems)
        setItemVisibility(itemIdExpirationDate, showExtraMenuItems)
    }
}
