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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.detail.DetailListAdapter.DetailViewHolder
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent
import com.pyamsoft.fridge.detail.item.DetailListItemDate
import com.pyamsoft.fridge.detail.item.DetailListItemGlances
import com.pyamsoft.fridge.detail.item.DetailListItemName
import com.pyamsoft.fridge.detail.item.DetailListItemPresence
import com.pyamsoft.fridge.detail.item.DetailListItemViewState
import com.pyamsoft.pydroid.arch.ViewBinder
import com.pyamsoft.pydroid.arch.bindViews
import com.pyamsoft.pydroid.arch.doOnDestroy
import com.pyamsoft.pydroid.ui.util.layout
import javax.inject.Inject

internal class DetailItemViewHolder internal constructor(
    itemView: View,
    owner: LifecycleOwner,
    editable: Boolean,
    callback: DetailListAdapter.Callback,
    componentCreator: DetailListItemComponentCreator
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

    private val viewBinder: ViewBinder<DetailListItemViewState>

    init {
        val parent = itemView.findViewById<ConstraintLayout>(R.id.detail_list_item)
        componentCreator.create(parent, editable).inject(this)

        val name = requireNotNull(nameView)
        val date = requireNotNull(dateView)
        val presence = requireNotNull(presenceView)
        val glances = requireNotNull(glancesView)

        viewBinder = bindViews(
            owner,
            name,
            date,
            presence,
            glances
        ) {
            return@bindViews when (it) {
                is DetailItemViewEvent.ExpandItem -> callback.onItemExpanded(adapterPosition)
                is DetailItemViewEvent.CommitPresence -> callback.onPresenceChange(adapterPosition)
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
        }
    }

    override fun bind(state: DetailListItemViewState) {
        viewBinder.bind(state)
    }
}
