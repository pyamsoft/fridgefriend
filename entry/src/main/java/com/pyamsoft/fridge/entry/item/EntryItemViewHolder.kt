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

package com.pyamsoft.fridge.entry.item

import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.entry.EntryListAdapter
import com.pyamsoft.fridge.entry.databinding.EntryListItemHolderBinding
import com.pyamsoft.pydroid.arch.ViewBinder
import com.pyamsoft.pydroid.arch.createViewBinder
import javax.inject.Inject

class EntryItemViewHolder internal constructor(
    binding: EntryListItemHolderBinding,
    factory: EntryItemComponent.Factory,
    callback: EntryListAdapter.Callback
) : RecyclerView.ViewHolder(binding.root), ViewBinder<EntryItemViewState> {

    @Inject
    @JvmField
    internal var scrimView: EntryListItemScrim? = null

    @Inject
    @JvmField
    internal var nameView: EntryListItemName? = null

    @Inject
    @JvmField
    internal var clickView: EntryListItemClick? = null

    private var viewBinder: ViewBinder<EntryItemViewState>

    init {
        factory.create(binding.entryListItem).inject(this)

        viewBinder = createViewBinder(
            requireNotNull(nameView),
            requireNotNull(scrimView),
            requireNotNull(clickView)
        ) {
            return@createViewBinder when (it) {
                is EntryItemViewEvent.ExpandEntry -> callback.onSelect(adapterPosition)
                is EntryItemViewEvent.EditEntry -> callback.onEdit(adapterPosition)
            }
        }
    }

    override fun bind(state: EntryItemViewState) {
        viewBinder.bind(state)
    }

    override fun teardown() {
        viewBinder.teardown()
        scrimView = null
        nameView = null
        clickView = null
    }

}
