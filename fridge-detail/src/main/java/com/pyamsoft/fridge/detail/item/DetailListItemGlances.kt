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
import android.widget.ImageView
import androidx.annotation.CheckResult
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import com.pyamsoft.fridge.db.daysLaterMidnight
import com.pyamsoft.fridge.db.isExpired
import com.pyamsoft.fridge.db.isExpiringSoon
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.ExpandItem
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.tintWith
import java.util.Calendar
import javax.inject.Inject

class DetailListItemGlances @Inject internal constructor(
    private val imageLoader: ImageLoader,
    parent: ViewGroup
) : BaseUiView<DetailItemViewState, DetailItemViewEvent>(parent) {

    override val layout: Int = R.layout.detail_list_item_glances

    override val layoutRoot by boundView<ViewGroup>(R.id.detail_item_glances)

    private val validExpirationDate by boundView<ImageView>(R.id.detail_item_glances_date)
    private val itemExpiringSoon by boundView<ImageView>(R.id.detail_item_glances_expiring)
    private val itemExpired by boundView<ImageView>(R.id.detail_item_glances_expired)

    private var dateRangeLoader: Loaded? = null
    private var expiringLoader: Loaded? = null
    private var expiredLoader: Loaded? = null

    init {
        doOnTeardown {
            layoutRoot.setOnDebouncedClickListener(null)
        }

        doOnTeardown {
            dateRangeLoader?.dispose()
            dateRangeLoader = null
        }

        doOnTeardown {
            expiringLoader?.dispose()
            expiringLoader = null
        }

        doOnTeardown {
            expiredLoader?.dispose()
            expiredLoader = null
        }
    }

    override fun onRender(
        state: DetailItemViewState,
        savedState: UiSavedState
    ) {
        state.item.let { item ->
            val isVisible = item.isReal() && !item.isArchived() && item.presence() == HAVE
            layoutRoot.isVisible = isVisible

            layoutRoot.setOnDebouncedClickListener {
                publish(ExpandItem(item))
            }

            val today = Calendar.getInstance()
            val soonDate = Calendar.getInstance().daysLaterMidnight(state.expirationRange)
            val isSameDayExpired = state.isSameDayExpired
            val isReal = item.isReal()
            setDateRangeView(item, isReal)
            setExpiringView(item, today, soonDate, isSameDayExpired, isReal)
            setExpiredView(item, today, isSameDayExpired, isReal)
        }
    }

    private fun setDateRangeView(item: FridgeItem, isReal: Boolean) {
        dateRangeLoader = setViewColor(
            imageLoader,
            validExpirationDate,
            R.drawable.ic_date_range_24dp,
            dateRangeLoader,
            item.expireTime() != null,
            isReal
        )
    }

    private fun setExpiringView(
        item: FridgeItem,
        today: Calendar,
        soonDate: Calendar,
        isSameDayExpired: Boolean,
        isReal: Boolean
    ) {
        expiringLoader = setViewColor(
            imageLoader,
            itemExpiringSoon,
            R.drawable.ic_consumed_24dp,
            expiringLoader,
            item.isExpiringSoon(today, soonDate, isSameDayExpired),
            isReal
        )
    }

    private fun setExpiredView(
        item: FridgeItem,
        today: Calendar,
        isSameDayExpired: Boolean,
        isReal: Boolean
    ) {
        expiredLoader = setViewColor(
            imageLoader,
            itemExpired,
            R.drawable.ic_spoiled_24dp,
            expiredLoader,
            item.isExpired(today, isSameDayExpired),
            isReal
        )
    }

    companion object {

        @CheckResult
        private fun setViewColor(
            imageLoader: ImageLoader,
            view: ImageView,
            @DrawableRes drawable: Int,
            loaded: Loaded?,
            isColored: Boolean,
            isReal: Boolean
        ): Loaded? {
            if (isReal) {
                val color = if (isColored) R.color.red500 else R.color.grey500
                loaded?.dispose()
                return imageLoader.load(drawable)
                    .mutate { it.tintWith(view.context, color) }
                    .into(view)
            }

            view.isVisible = false
            return loaded
        }
    }
}
