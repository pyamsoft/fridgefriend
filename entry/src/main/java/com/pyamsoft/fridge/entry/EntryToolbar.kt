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

package com.pyamsoft.fridge.entry

import com.pyamsoft.fridge.ui.view.UiToolbar
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import javax.inject.Inject

class EntryToolbar @Inject internal constructor(
    toolbarActivity: ToolbarActivity,
) : UiToolbar<EntryViewState.Sorts, EntryViewState, EntryViewEvent>(
    withToolbar = { toolbarActivity.withToolbar(it) }
) {

    init {
        doOnInflate {
            toolbarActivity.withToolbar { it.setUpEnabled(false) }
        }

        doOnTeardown {
            toolbarActivity.withToolbar { it.setUpEnabled(false) }
        }
    }

    override fun publishSearchEvent(search: String) {
        publish(EntryViewEvent.SearchQuery(search))
    }

    override fun publishSortEvent(sort: State.Sort<EntryViewState.Sorts>) {
        publish(EntryViewEvent.ChangeSort(sort.original))
    }

    override fun onGetSortForMenuItem(itemId: Int): EntryViewState.Sorts? {
        return when (itemId) {
            itemIdCreatedDate -> EntryViewState.Sorts.CREATED
            itemIdName -> EntryViewState.Sorts.NAME
            else -> null
        }
    }
}
