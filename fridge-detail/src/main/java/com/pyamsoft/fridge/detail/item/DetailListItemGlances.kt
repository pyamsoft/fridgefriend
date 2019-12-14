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
import com.pyamsoft.fridge.core.tooltip.Tooltip
import com.pyamsoft.fridge.core.tooltip.TooltipCreator
import com.pyamsoft.fridge.db.cleanMidnight
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

class DetailListItemGlances @Inject internal constructor(
    private val tooltipCreator: TooltipCreator,
    private val imageLoader: ImageLoader,
    parent: ViewGroup
) : BaseUiView<DetailListItemViewState, DetailItemViewEvent>(parent) {

    override val layout: Int = R.layout.detail_list_item_glances

    override val layoutRoot by boundView<ViewGroup>(R.id.detail_item_glances)

    private val validExpirationDate by boundView<ImageView>(R.id.detail_item_glances_date)
    private val itemExpiringSoon by boundView<ImageView>(R.id.detail_item_glances_expiring)
    private val itemExpired by boundView<ImageView>(R.id.detail_item_glances_expired)

    private var dateRangeLoader: Loaded? = null
    private var expiringLoader: Loaded? = null
    private var expiredLoader: Loaded? = null

    private var dateRangeTooltip: Tooltip? = null
    private var expiringTooltip: Tooltip? = null
    private var expiredTooltip: Tooltip? = null

    init {
        doOnInflate {
            layoutRoot.setOnDebouncedClickListener { publish(ExpandItem) }
            validExpirationDate.setOnDebouncedClickListener { dateRangeTooltip?.show(it) }
            itemExpiringSoon.setOnDebouncedClickListener { expiringTooltip?.show(it) }
            itemExpired.setOnDebouncedClickListener { expiredTooltip?.show(it) }
        }

        doOnTeardown {
            layoutRoot.setOnDebouncedClickListener(null)
            validExpirationDate.setOnDebouncedClickListener(null)
            itemExpiringSoon.setOnDebouncedClickListener(null)
            itemExpired.setOnDebouncedClickListener(null)
            clear()
        }
    }

    private fun clear() {
        dateRangeLoader?.dispose()
        dateRangeLoader = null
        dateRangeTooltip?.hide()
        dateRangeTooltip = null

        expiringLoader?.dispose()
        expiringLoader = null
        expiringTooltip?.hide()
        expiringTooltip = null

        expiredLoader?.dispose()
        expiredLoader = null
        expiredTooltip?.hide()
        expiredTooltip = null
    }

    override fun onRender(
        state: DetailListItemViewState,
        savedState: UiSavedState
    ) {
        state.item.let { item ->
            val isVisible = item.isReal() && !item.isArchived() && item.presence() == HAVE
            layoutRoot.isVisible = isVisible

            val today = Calendar.getInstance().cleanMidnight()
            val soonDate = Calendar.getInstance().daysLaterMidnight(state.expirationRange)
            val isSameDayExpired = state.isSameDayExpired
            val expireTime = item.expireTime()
            val hasTime = expireTime != null
            val isExpiringSoon = item.isExpiringSoon(today, soonDate, isSameDayExpired)
            val isExpired = item.isExpired(today, isSameDayExpired)

            setDateRangeView(item, expireTime, hasTime)
            setExpiringView(item, expireTime, today, isExpiringSoon, isExpired, hasTime)
            setExpiredView(item, expireTime, today, isExpired, hasTime)
        }
    }

    private fun setDateRangeView(
        item: FridgeItem, expireTime: Date?,
        hasTime: Boolean
    ) {
        dateRangeLoader = setViewColor(
            imageLoader,
            validExpirationDate,
            R.drawable.ic_date_range_24dp,
            dateRangeLoader,
            hasTime,
            hasTime
        )

        dateRangeTooltip?.hide()
        if (expireTime == null) {
            dateRangeTooltip = null
            return
        }

        dateRangeTooltip = tooltipCreator.top {
            dismissOnClick()
            dismissOnClickOutside()
            setArrowPosition(0.745F)

            val dateFormatted = SimpleDateFormat.getDateInstance().format(expireTime)
            setText("${item.name().trim()} will expire on $dateFormatted")
        }
    }

    private fun setExpiringView(
        item: FridgeItem,
        expireTime: Date?,
        today: Calendar,
        isExpiringSoon: Boolean,
        isExpired: Boolean,
        hasTime: Boolean
    ) {
        // Show this if there is a time and the item is not yet expired
        val isVisible = hasTime && !isExpired

        expiringLoader = setViewColor(
            imageLoader,
            itemExpiringSoon,
            R.drawable.ic_consumed_24dp,
            expiringLoader,
            isExpiringSoon,
            isVisible
        )

        expiringTooltip?.hide()
        if (!isVisible || !hasTime) {
            expiringTooltip = null
            return
        }

        val expireCalendar = Calendar.getInstance()
            .apply { time = requireNotNull(expireTime) }
            .cleanMidnight()
        expiringTooltip = tooltipCreator.top {
            dismissOnClick()
            dismissOnClickOutside()
            setArrowPosition(0.86F)

            // shitty old time format parser for very basic expiration estimate
            val todayTime = today.timeInMillis
            val expiringTime = expireCalendar.timeInMillis

            val difference = expiringTime - todayTime
            val seconds = difference / 1000L
            val minutes = seconds / 60L
            val hours = minutes / 60L
            val days = hours / 24L

            require(days >= 0)
            val expirationRange = if (days == 0L) "will expire today" else {
                if (days < 7) {
                    "will expire in $days ${if (days == 1L) "day" else "days"}"
                } else {
                    val weeks = days / 7L
                    if (weeks < WEEK_LIMIT) {
                        "will expire in $weeks ${if (weeks == 1L) "week" else "weeks"}"
                    } else {
                        "doesn't expire for a long time"
                    }
                }
            }

            setText("${item.name().trim()} $expirationRange")
        }
    }

    private fun setExpiredView(
        item: FridgeItem,
        expireTime: Date?,
        today: Calendar,
        isExpired: Boolean,
        hasTime: Boolean
    ) {
        // Show this if there is a time and the item is expired
        val isVisible = hasTime && isExpired

        expiredLoader = setViewColor(
            imageLoader,
            itemExpired,
            R.drawable.ic_spoiled_24dp,
            expiredLoader,
            isExpired,
            isVisible
        )

        expiredTooltip?.hide()
        if (!isVisible) {
            expiredTooltip = null
            return
        }

        val expireCalendar = Calendar.getInstance()
            .apply { time = requireNotNull(expireTime) }
            .cleanMidnight()
        expiredTooltip = tooltipCreator.top {
            dismissOnClick()
            dismissOnClickOutside()
            setArrowPosition(0.85F)

            // shitty old time format parser for very basic expiration estimate
            val todayTime = today.timeInMillis
            val expiringTime = expireCalendar.timeInMillis

            val difference = todayTime - expiringTime
            val seconds = difference / 1000L
            val minutes = seconds / 60L
            val hours = minutes / 60L
            val days = hours / 24L

            require(days >= 0)
            val expirationRange = if (days == 0L) "today" else {
                if (days < 7) {
                    "$days ${if (days == 1L) "day" else "days"} ago"
                } else {
                    val weeks = days / 7L
                    if (weeks < WEEK_LIMIT) {
                        "$weeks ${if (weeks == 1L) "week" else "weeks"} ago"
                    } else {
                        "a long time ago"
                    }
                }
            }

            setText("${item.name().trim()} expired $expirationRange")
        }
    }

    companion object {

        // TODO: Make preference
        private const val WEEK_LIMIT = 10

        @JvmStatic
        @CheckResult
        private fun setViewColor(
            imageLoader: ImageLoader,
            view: ImageView,
            @DrawableRes drawable: Int,
            loaded: Loaded?,
            isColored: Boolean,
            isVisible: Boolean
        ): Loaded? {
            if (isVisible) {
                view.isVisible = true
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
