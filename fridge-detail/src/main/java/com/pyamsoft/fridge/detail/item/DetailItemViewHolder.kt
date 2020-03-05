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
 *
 */

package com.pyamsoft.fridge.detail.item

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.detail.databinding.DetailListItemHolderBinding
import com.pyamsoft.pydroid.arch.UiView
import com.pyamsoft.pydroid.arch.ViewBinder
import com.pyamsoft.pydroid.arch.bindViews
import com.pyamsoft.pydroid.arch.doOnDestroy
import com.pyamsoft.pydroid.ui.util.layout
import javax.inject.Inject

abstract class DetailItemViewHolder protected constructor(
    binding: DetailListItemHolderBinding,
    private val owner: LifecycleOwner,
    private val callback: DetailListAdapter.Callback
) : DetailViewHolder<DetailListItemHolderBinding>(binding) {

    @JvmField
    @Inject
    internal var nameView: DetailListItemName? = null

    @JvmField
    @Inject
    internal var presenceView: DetailListItemPresence? = null

    private var viewBinder: ViewBinder<DetailListItemViewState>? = null

    protected fun create(
        parent: ConstraintLayout,
        extra: UiView<DetailListItemViewState, DetailItemViewEvent>
    ) {
        val name = requireNotNull(nameView)
        val presence = requireNotNull(presenceView)
        viewBinder = bindViews(
            owner,
            name,
            extra,
            presence
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
                connect(it.id(), ConstraintSet.END, extra.id(), ConstraintSet.START)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }
        }

        owner.doOnDestroy {
            nameView = null
            presenceView = null
            viewBinder = null
        }
    }

    final override fun bind(state: DetailListItemViewState) {
        requireNotNull(viewBinder).bind(state)
    }
}
