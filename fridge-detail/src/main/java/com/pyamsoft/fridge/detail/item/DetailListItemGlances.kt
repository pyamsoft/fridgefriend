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
import com.pyamsoft.fridge.core.today
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.cleanMidnight
import com.pyamsoft.fridge.db.item.daysLaterMidnight
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.db.item.isExpired
import com.pyamsoft.fridge.db.item.isExpiringSoon
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.databinding.DetailListItemGlancesBinding
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.ExpandItem
import com.pyamsoft.fridge.tooltip.Tooltip
import com.pyamsoft.fridge.tooltip.TooltipCreator
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.tintWith
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToLong

class DetailListItemGlances @Inject internal constructor(
    private val tooltipCreator: TooltipCreator,
    private val theming: ThemeProvider,
    private val imageLoader: ImageLoader,
    parent: ViewGroup
) : BaseUiView<DetailListItemViewState, DetailItemViewEvent, DetailListItemGlancesBinding>(parent) {

    override val viewBinding = DetailListItemGlancesBinding::inflate

    override val layoutRoot by boundView { detailItemGlances }

    private var dateRangeLoader: Loaded? = null
    private var expiringLoader: Loaded? = null
    private var expiredLoader: Loaded? = null

    private var dateRangeTooltip: Tooltip? = null
    private var expiringTooltip: Tooltip? = null
    private var expiredTooltip: Tooltip? = null

    init {
        doOnInflate {
            layoutRoot.setOnDebouncedClickListener { publish(ExpandItem) }
            binding.detailItemGlancesDate.setOnDebouncedClickListener { dateRangeTooltip?.show(it) }
            binding.detailItemGlancesExpiring.setOnDebouncedClickListener { expiringTooltip?.show(it) }
            binding.detailItemGlancesExpired.setOnDebouncedClickListener { expiredTooltip?.show(it) }
        }

        doOnTeardown {
            layoutRoot.setOnDebouncedClickListener(null)
            binding.detailItemGlancesDate.setOnDebouncedClickListener(null)
            binding.detailItemGlancesExpiring.setOnDebouncedClickListener(null)
            binding.detailItemGlancesExpired.setOnDebouncedClickListener(null)
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

    override fun onRender(state: DetailListItemViewState) {
        state.item.let { item ->
            assert(item.isReal()) { "Cannot render non-real item: $item" }
            val range = state.expirationRange
            val isSameDayExpired = state.isSameDayExpired

            val isVisible =
                !item.isArchived() && item.presence() == HAVE && range != null && isSameDayExpired != null
            layoutRoot.isVisible = isVisible

            if (isVisible) {
                // This should be fine because of the isVisible conditional
                val dateRange = requireNotNull(range).range
                val isSameDay = requireNotNull(isSameDayExpired).isSame

                val now = today().cleanMidnight()
                val soonDate = today().daysLaterMidnight(dateRange)
                val expireTime = item.expireTime()
                val hasTime = expireTime != null
                val isExpiringSoon = item.isExpiringSoon(now, soonDate, isSameDay)
                val isExpired = item.isExpired(now, isSameDay)

                setDateRangeView(item, expireTime, hasTime)
                setExpiringView(item, expireTime, now, isExpiringSoon, isExpired, hasTime)
                setExpiredView(item, expireTime, now, isExpired, hasTime)
            }
        }
    }

    private fun setDateRangeView(
        item: FridgeItem,
        expireTime: Date?,
        hasTime: Boolean
    ) {
        dateRangeLoader = setViewColor(
            theming,
            imageLoader,
            binding.detailItemGlancesDate,
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
            setArrowPosition(0.81F)

            val dateFormatted = SimpleDateFormat.getDateInstance().format(expireTime)
            setText("${item.name().trim()} will expire on $dateFormatted")
        }
    }

    private fun setExpiringView(
        item: FridgeItem,
        expireTime: Date?,
        now: Calendar,
        isExpiringSoon: Boolean,
        isExpired: Boolean,
        hasTime: Boolean
    ) {
        // Show this if there is a time and the item is not yet expired
        val isVisible = hasTime && !isExpired

        expiringLoader = setViewColor(
            theming,
            imageLoader,
            binding.detailItemGlancesExpiring,
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

        val expireCalendar = today()
            .apply { time = requireNotNull(expireTime) }
            .cleanMidnight()

        expiringTooltip = tooltipCreator.top {
            dismissOnClick()
            dismissOnClickOutside()
            setArrowPosition(0.90F)

            // shitty old time format parser for very basic expiration estimate
            val todayTime = now.timeInMillis
            val expiringTime = expireCalendar.timeInMillis
            val days = getDaysToTime(todayTime, expiringTime)

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
        now: Calendar,
        isExpired: Boolean,
        hasTime: Boolean
    ) {
        // Show this if there is a time and the item is expired
        val isVisible = hasTime && isExpired

        expiredLoader = setViewColor(
            theming,
            imageLoader,
            binding.detailItemGlancesExpired,
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

        val expireCalendar = today()
            .apply { time = requireNotNull(expireTime) }
            .cleanMidnight()
        expiredTooltip = tooltipCreator.top {
            dismissOnClick()
            dismissOnClickOutside()
            setArrowPosition(0.89F)

            // shitty old time format parser for very basic expiration estimate
            val todayTime = now.timeInMillis
            val expiringTime = expireCalendar.timeInMillis
            val days = getDaysToTime(expiringTime, todayTime)

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

        private const val WEEK_LIMIT = 10

        @JvmStatic
        @CheckResult
        private fun setViewColor(
            theming: ThemeProvider,
            imageLoader: ImageLoader,
            view: ImageView,
            @DrawableRes drawable: Int,
            loaded: Loaded?,
            isColored: Boolean,
            isVisible: Boolean
        ): Loaded? {
            if (isVisible) {
                view.isVisible = true
                val color =
                    if (isColored) R.color.colorSecondary else if (theming.isDarkTheme()) R.color.white else R.color.black
                loaded?.dispose()
                return imageLoader.load(drawable)
                    .mutate { it.tintWith(view.context, color) }
                    .into(view)
            }

            view.isVisible = false
            return loaded
        }

        @JvmStatic
        @CheckResult
        private fun getDaysToTime(nowTime: Long, expireTime: Long): Long {
            val difference = expireTime - nowTime
            val seconds = (difference.toFloat() / 1000L).roundToLong()
            val minutes = (seconds.toFloat() / 60L).roundToLong()
            val hours = (minutes.toFloat() / 60L).roundToLong()
            return (hours.toFloat() / 24L).roundToLong()
        }
    }
}
