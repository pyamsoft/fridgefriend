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

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.detail.R
import javax.inject.Inject

class DetailItemDateViewHolder internal constructor(
    itemView: View,
    owner: LifecycleOwner,
    editable: Boolean,
    callback: DetailListAdapter.Callback,
    factory: DetailItemComponent.Factory
) : DetailItemViewHolder(itemView, owner, callback) {

    @JvmField
    @Inject
    internal var dateView: DetailListItemDate? = null

    init {
        val parent = itemView.findViewById<ConstraintLayout>(R.id.detail_list_item)
        factory.create(parent, editable).inject(this)

        val date = requireNotNull(dateView)
        create(parent, date)
    }
}
