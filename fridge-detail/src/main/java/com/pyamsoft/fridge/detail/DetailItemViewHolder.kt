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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.ViewModelStore
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailListAdapter.Callback
import com.pyamsoft.fridge.detail.DetailListAdapter.DetailViewHolder
import com.pyamsoft.fridge.detail.item.DetailItemComponent
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.DatePick
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.ExpandDetails
import com.pyamsoft.fridge.detail.item.DetailListItemDate
import com.pyamsoft.fridge.detail.item.DetailListItemGlances
import com.pyamsoft.fridge.detail.item.DetailListItemName
import com.pyamsoft.fridge.detail.item.DetailListItemPresence
import com.pyamsoft.fridge.detail.item.DetailListItemViewModel
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import com.pyamsoft.pydroid.ui.arch.factory
import com.pyamsoft.pydroid.ui.util.layout
import javax.inject.Inject

internal class DetailItemViewHolder internal constructor(
    itemView: View,
    owner: LifecycleOwner,
    editable: Boolean,
    callback: Callback,
    injectComponent: (parent: ViewGroup, editable: Boolean) -> DetailItemComponent
) : DetailViewHolder(itemView) {

    @JvmField
    @Inject
    internal var nameView: DetailListItemName? = null
    @JvmField
    @Inject
    internal var dateView: DetailListItemDate? = null
    @JvmField
    @Inject
    internal var presenceView: DetailListItemPresence? = null
    @JvmField
    @Inject
    internal var glancesView: DetailListItemGlances? = null

    @JvmField
    @Inject
    internal var factory: Factory? = null
    private val viewModel by factory<DetailListItemViewModel>(ViewModelStore()) { factory }

    init {
        val parent = itemView.findViewById<ConstraintLayout>(R.id.detail_list_item)
        injectComponent(parent, editable)
            .inject(this)

        val name = requireNotNull(nameView)
        val date = requireNotNull(dateView)
        val presence = requireNotNull(presenceView)
        val glances = requireNotNull(glancesView)

        createComponent(
            null, owner,
            viewModel,
            name,
            date,
            presence,
            glances
        ) {
            return@createComponent when (it) {
                is ExpandDetails -> callback.onItemExpanded(it.item)
                is DatePick -> callback.onPickDate(it.oldItem, it.year, it.month, it.day)
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
                    it.id(), ConstraintSet.START,
                    presence.id(),
                    ConstraintSet.END
                )
                connect(it.id(), ConstraintSet.END, date.id(), ConstraintSet.START)
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
                    it.id(), ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END
                )
                constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }
        }

        owner.doOnDestroy {
            nameView = null
            glancesView = null
            dateView = null
            presenceView = null
            factory = null
        }
    }

    override fun bind(item: FridgeItem) {
        viewModel.bind(item)
    }

    override fun unbind() {
        viewModel.unbind()
    }

    fun consume() {
        viewModel.consume()
    }

    fun spoil() {
        viewModel.spoil()
    }

    fun restore() {
        viewModel.restore()
    }

    fun delete() {
        viewModel.delete()
    }
}
