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

package com.pyamsoft.fridge.detail.expand

import android.view.ViewGroup
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.detail.base.BaseItemDate
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named

class ExpandItemDate @Inject internal constructor(
    @Named("item_editable") private val isEditable: Boolean,
    imageLoader: ImageLoader,
    theming: ThemeProvider,
    parent: ViewGroup
) : BaseItemDate<ExpandItemViewState, ExpandedItemViewEvent>(imageLoader, theming, parent) {

    override fun onRender(state: ExpandItemViewState, savedState: UiSavedState) {
        val item = state.item
        baseRender(item)
        if (!isEditable) {
            return
        }

        val expireTime = item.expireTime()
        val month: Int
        val day: Int
        val year: Int

        if (expireTime != null) {
            val date = Calendar.getInstance()
                .apply { time = expireTime }
            Timber.d("Expire time is: $date")

            // Month is zero indexed in storage
            month = date.get(Calendar.MONTH)
            day = date.get(Calendar.DAY_OF_MONTH)
            year = date.get(Calendar.YEAR)
        } else {
            month = 0
            day = 0
            year = 0
        }

        if (!item.isArchived()) {
            layoutRoot.setOnDebouncedClickListener {
                publish(
                    ExpandedItemViewEvent.PickDate(
                        item,
                        year,
                        month,
                        day
                    )
                )
            }
        } else {
            layoutRoot.setOnDebouncedClickListener(null)
        }
    }
}
