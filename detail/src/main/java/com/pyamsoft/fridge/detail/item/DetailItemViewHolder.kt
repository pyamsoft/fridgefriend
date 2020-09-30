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

package com.pyamsoft.fridge.detail.item

import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.detail.databinding.DetailListItemHolderBinding
import com.pyamsoft.pydroid.arch.ViewBinder
import com.pyamsoft.pydroid.arch.bindViews
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.util.doOnDestroy
import javax.inject.Inject

class DetailItemViewHolder internal constructor(
    binding: DetailListItemHolderBinding,
    owner: LifecycleOwner,
    editable: Boolean,
    callback: DetailListAdapter.Callback,
    factory: DetailItemComponent.Factory
) : RecyclerView.ViewHolder(binding.root), ViewBinder<DetailItemViewState> {

    @JvmField
    @Inject
    internal var nameView: DetailListItemName? = null

    @JvmField
    @Inject
    internal var presenceView: DetailListItemPresence? = null

    @JvmField
    @Inject
    internal var extraContainer: DetailListItemContainer? = null

    @JvmField
    @Inject
    internal var countView: DetailListItemCount? = null

    private val viewBinder: ViewBinder<DetailItemViewState>

    init {
        factory.create(binding.detailListItem, owner, editable).inject(this)
        val count = requireNotNull(countView)
        val extra = requireNotNull(extraContainer)
        val name = requireNotNull(nameView)
        val presence = requireNotNull(presenceView)
        viewBinder = bindViews(
            owner,
            name,
            extra,
            count,
            presence
        ) {
            return@bindViews when (it) {
                is DetailItemViewEvent.ExpandItem -> callback.onItemExpanded(adapterPosition)
                is DetailItemViewEvent.CommitPresence -> callback.onPresenceChange(adapterPosition)
                is DetailItemViewEvent.IncreaseCount -> callback.onIncreaseCount(adapterPosition)
                is DetailItemViewEvent.DecreaseCount -> callback.onDecreaseCount(adapterPosition)
            }
        }

        binding.detailListItem.layout {
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

            extra.also {
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
                connect(
                    it.id(), ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
                constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
                constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }

            count.also {
                connect(
                    it.id(), ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP
                )
                connect(
                    it.id(), ConstraintSet.END,
                    extra.id(),
                    ConstraintSet.START
                )
                connect(
                    it.id(), ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
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
                connect(it.id(), ConstraintSet.END, count.id(), ConstraintSet.START)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }
        }

        owner.doOnDestroy {
            nameView = null
            presenceView = null
            extraContainer = null
            countView = null
        }
    }

    override fun bind(state: DetailItemViewState) {
        viewBinder.bind(state)
    }

}
