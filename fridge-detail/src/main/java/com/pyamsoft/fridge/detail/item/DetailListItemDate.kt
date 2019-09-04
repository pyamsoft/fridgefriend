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

package com.pyamsoft.fridge.detail.item

import android.view.ViewGroup
import android.widget.TextView
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.ExpandItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.PickDate
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named

class DetailListItemDate @Inject internal constructor(
    parent: ViewGroup,
    @Named("item_editable") private val isEditable: Boolean
) : BaseUiView<DetailItemViewState, DetailItemViewEvent>(parent) {

    override val layout: Int = R.layout.detail_list_item_date

    override val layoutRoot by boundView<TextView>(R.id.detail_item_date)

    override fun onRender(
        state: DetailItemViewState,
        savedState: UiSavedState
    ) {
        state.item.let { item ->
            val month: Int
            val day: Int
            val year: Int
            val expireTime = item.expireTime()
            if (expireTime != null) {
                val date = Calendar.getInstance()
                    .apply { time = expireTime }
                Timber.d("Expire time is: $date")

                // Month is zero indexed in storage
                month = date.get(Calendar.MONTH)
                day = date.get(Calendar.DAY_OF_MONTH)
                year = date.get(Calendar.YEAR)

                val dateString =
                    "${"${month + 1}".padStart(2, '0')}/${
                    "$day".padStart(2, '0')}/${
                    "$year".padStart(4, '0')}"
                layoutRoot.text = dateString
            } else {
                month = 0
                day = 0
                year = 0
                layoutRoot.text = "__/__/____"
            }

            if (!item.isArchived()) {
                if (isEditable) {
                    layoutRoot.setOnDebouncedClickListener {
                        publish(PickDate(item, year, month, day))
                    }
                } else {
                    layoutRoot.setOnDebouncedClickListener {
                        publish(ExpandItem(item))
                    }
                }
            }
        }
    }

    override fun onTeardown() {
        layoutRoot.setOnDebouncedClickListener(null)
    }
}
