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

package com.pyamsoft.fridge.detail

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.ViewModelStore
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailListAdapter.Callback
import com.pyamsoft.fridge.detail.DetailListAdapter.DetailViewHolder
import com.pyamsoft.fridge.detail.R.id
import com.pyamsoft.fridge.detail.item.DetailItemComponent
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.CloseExpand
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.DatePick
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.ExpandDetails
import com.pyamsoft.fridge.detail.item.DetailListItemDate
import com.pyamsoft.fridge.detail.item.DetailListItemGlances
import com.pyamsoft.fridge.detail.item.DetailListItemName
import com.pyamsoft.fridge.detail.item.DetailListItemPresence
import com.pyamsoft.fridge.detail.item.DetailListItemViewModel
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.app.ListItemLifecycle
import com.pyamsoft.pydroid.ui.arch.factory
import com.pyamsoft.pydroid.ui.util.layout
import timber.log.Timber
import javax.inject.Inject

internal class DetailItemViewHolder internal constructor(
    itemView: View,
    private val injectComponent: (parent: ViewGroup, item: FridgeItem, editable: Boolean) -> DetailItemComponent
) : DetailViewHolder(itemView) {

    @JvmField
    @Inject
    internal var factory: Factory? = null
    @JvmField
    @Inject
    internal var name: DetailListItemName? = null
    @JvmField
    @Inject
    internal var date: DetailListItemDate? = null
    @JvmField
    @Inject
    internal var presence: DetailListItemPresence? = null
    @JvmField
    @Inject
    internal var glances: DetailListItemGlances? = null
    private var viewModel: DetailListItemViewModel? = null

    private val parent: ConstraintLayout = itemView.findViewById(id.listitem_constraint)

    private var lifecycle: ListItemLifecycle? = null
    private var boundItem: FridgeItem? = null

    private fun injectViewModel(lifecycle: Lifecycle) {
        viewModel = lifecycle.factory<DetailListItemViewModel>(ViewModelStore()) { factory }
            .get()
    }

    override fun bind(
        item: FridgeItem,
        editable: Boolean,
        callback: Callback
    ) {
        boundItem = item
        val owner = ListItemLifecycle()
        lifecycle?.unbind()
        lifecycle = owner

        injectComponent(parent, item, editable)
            .inject(this)
        injectViewModel(owner.lifecycle)

        val name = requireNotNull(name)
        val date = requireNotNull(date)
        val presence = requireNotNull(presence)
        val glances = requireNotNull(glances)

        createComponent(
            null, owner,
            requireNotNull(viewModel),
            name,
            date,
            presence,
            glances
        ) {
            return@createComponent when (it) {
                is ExpandDetails -> callback.onItemExpanded(
                    it.item
                )
                is DatePick -> callback.onPickDate(
                    it.oldItem, it.year, it.month, it.day
                )
                is CloseExpand -> Timber.d(
                    "Deleted item"
                )
            }
        }

        parent.layout {
            presence.also {
                connect(
                    it.id(), ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP
                )
                connect(
                    it.id(), ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
                connect(
                    it.id(), ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START
                )
                constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            date.also {
                connect(
                    it.id(), ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP
                )
                connect(
                    it.id(), ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END
                )
                constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            name.also {
                connect(
                    it.id(), ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP
                )
                connect(
                    it.id(), ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
                connect(
                    it.id(), ConstraintSet.START, presence.id(),
                    ConstraintSet.END
                )
                connect(
                    it.id(), ConstraintSet.END, date.id(),
                    ConstraintSet.START
                )
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            glances.also {
                connect(
                    it.id(), ConstraintSet.TOP, date.id(),
                    ConstraintSet.BOTTOM
                )
                connect(
                    it.id(), ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
                connect(
                    it.id(), ConstraintSet.START, date.id(),
                    ConstraintSet.START
                )
                connect(
                    it.id(), ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END
                )
                constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }
        }

        owner.bind()
    }

    override fun unbind() {
        lifecycle?.unbind()
        lifecycle = null

        viewModel = null
        boundItem = null
        name = null
        date = null
        presence = null
    }

    // Kind of hacky
    fun consume() {
        requireNotNull(viewModel).consume()
    }

    // Kind of hacky
    fun spoil() {
        requireNotNull(viewModel).spoil()
    }

    // Kind of hacky
    fun delete() {
        requireNotNull(viewModel).delete()
    }
}
