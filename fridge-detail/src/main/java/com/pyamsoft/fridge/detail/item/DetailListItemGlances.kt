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
import javax.inject.Inject

class DetailListItemGlances @Inject internal constructor(
    private val tooltipCreator: TooltipCreator,
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

    private var dateRangeTooltip: Tooltip? = null
    private var expiringTooltip: Tooltip? = null
    private var expiredTooltip: Tooltip? = null

    init {
        doOnTeardown {
            layoutRoot.setOnDebouncedClickListener(null)
        }

        doOnTeardown {
            dateRangeLoader?.dispose()
            dateRangeLoader = null
            dateRangeTooltip?.hide()
            dateRangeTooltip = null
            validExpirationDate.setOnDebouncedClickListener(null)
        }

        doOnTeardown {
            expiringLoader?.dispose()
            expiringLoader = null
            expiringTooltip?.hide()
            expiringTooltip = null
            itemExpiringSoon.setOnDebouncedClickListener(null)
        }

        doOnTeardown {
            expiredLoader?.dispose()
            expiredLoader = null
            expiredTooltip?.hide()
            expiredTooltip = null
            itemExpired.setOnDebouncedClickListener(null)
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

            val today = Calendar.getInstance().cleanMidnight()
            val soonDate = Calendar.getInstance().daysLaterMidnight(state.expirationRange)
            val isSameDayExpired = state.isSameDayExpired
            val hasTime = item.expireTime() != null
            setDateRangeView(item, hasTime)
            setExpiringView(item, today, soonDate, isSameDayExpired, hasTime)
            setExpiredView(item, today, isSameDayExpired, hasTime)
        }
    }

    private fun setDateRangeView(item: FridgeItem, hasTime: Boolean) {
        val expireTime = item.expireTime()
        dateRangeLoader = setViewColor(
            imageLoader,
            validExpirationDate,
            R.drawable.ic_date_range_24dp,
            dateRangeLoader,
            expireTime != null,
            hasTime
        )

        dateRangeTooltip?.hide()
        if (expireTime == null) {
            dateRangeTooltip = null
            validExpirationDate.setOnDebouncedClickListener(null)
        } else {
            dateRangeTooltip = tooltipCreator.top {
                dismissOnClick()
                dismissOnClickOutside()
                setArrowPosition(0.73F)

                val dateFormatted = SimpleDateFormat.getDateInstance().format(expireTime)
                setText("${item.name().trim()} expires on $dateFormatted")
            }

            validExpirationDate.setOnDebouncedClickListener { dateRangeTooltip?.show(it) }
        }
    }

    private fun setExpiringView(
        item: FridgeItem,
        today: Calendar,
        soonDate: Calendar,
        isSameDayExpired: Boolean,
        hasTime: Boolean
    ) {
        expiringLoader = setViewColor(
            imageLoader,
            itemExpiringSoon,
            R.drawable.ic_consumed_24dp,
            expiringLoader,
            item.isExpiringSoon(today, soonDate, isSameDayExpired),
            hasTime
        )

        expiringTooltip?.hide()
        val expireTime = item.expireTime()
        val expireCalendar = if (expireTime == null) null else {
            Calendar.getInstance()
                .apply { time = expireTime }
                .cleanMidnight()
        }
        if (expireCalendar == null) {
            expiringTooltip = null
            itemExpiringSoon.setOnDebouncedClickListener(null)
        } else {
            expiringTooltip = tooltipCreator.top {
                dismissOnClick()
                dismissOnClickOutside()
                setArrowPosition(0.82F)

                val currentYear = today.get(Calendar.YEAR)
                val currentMonth = today.get(Calendar.MONTH)
                val currentDay = today.get(Calendar.DAY_OF_YEAR)

                val expiringYear = expireCalendar.get(Calendar.YEAR)
                val expiringMonth = expireCalendar.get(Calendar.MONTH)
                val expiringDay = expireCalendar.get(Calendar.DAY_OF_YEAR)

                val expirationRange = if (expiringYear > currentYear) {
                    val year = expiringYear - currentYear
                    "in $year ${if (year == 1) "year" else "years"}"
                } else if (expiringMonth > currentMonth) {
                    val month = expiringMonth - currentMonth
                    "in $month ${if (month == 1) "month" else "months"}"
                } else if (expiringDay > currentDay) {
                    val day = expiringDay - currentDay
                    val week = day / 7
                    if (week > 0) {
                        "in $week ${if (week == 1) "week" else "weeks"}"
                    } else {
                        "in $day ${if (day == 1) "day" else "days"}"
                    }
                } else if (expiringYear == currentYear && expiringMonth == currentMonth && expiringDay == currentDay) {
                    "today"
                } else {
                    "someday"
                }

                setText("${item.name().trim()} expires $expirationRange")
            }
            itemExpiringSoon.setOnDebouncedClickListener { expiringTooltip?.show(it) }
        }
    }

    private fun setExpiredView(
        item: FridgeItem,
        today: Calendar,
        isSameDayExpired: Boolean,
        hasTime: Boolean
    ) {
        val isExpired = item.isExpired(today, isSameDayExpired)
        expiredLoader = setViewColor(
            imageLoader,
            itemExpired,
            R.drawable.ic_spoiled_24dp,
            expiredLoader,
            isExpired,
            hasTime
        )

        expiredTooltip?.hide()
        if (!isExpired) {
            expiredTooltip = null
            itemExpired.setOnDebouncedClickListener(null)
        } else {
            val expireTime = item.expireTime()
            val expireCalendar = if (expireTime == null) null else {
                Calendar.getInstance()
                    .apply { time = expireTime }
                    .cleanMidnight()
            }
            if (expireCalendar == null) {
                expiredTooltip = null
                itemExpired.setOnDebouncedClickListener(null)
            } else {
                expiredTooltip = tooltipCreator.top {
                    dismissOnClick()
                    dismissOnClickOutside()
                    setArrowPosition(0.90F)

                    val currentYear = today.get(Calendar.YEAR)
                    val currentMonth = today.get(Calendar.MONTH)
                    val currentDay = today.get(Calendar.DAY_OF_YEAR)

                    val expiringYear = expireCalendar.get(Calendar.YEAR)
                    val expiringMonth = expireCalendar.get(Calendar.MONTH)
                    val expiringDay = expireCalendar.get(Calendar.DAY_OF_YEAR)

                    val expirationRange = if (expiringYear < currentYear) {
                        val year = expiringYear - currentYear
                        "$year ${if (year == 1) "year" else "years"} ago"
                    } else if (expiringMonth < currentMonth) {
                        val month = expiringMonth - currentMonth
                        "$month ${if (month == 1) "month" else "months"} ago"
                    } else if (expiringDay < currentDay) {
                        val day = expiringDay - currentDay
                        val week = day / 7
                        if (week > 0) {
                            "$week ${if (week == 1) "week" else "weeks"} ago"
                        } else {
                            "$day ${if (day == 1) "day" else "days"} ago"
                        }
                    } else if (expiringYear == currentYear && expiringMonth == currentMonth && expiringDay == currentDay) {
                        "today"
                    } else {
                        "someday"
                    }

                    setText("${item.name().trim()} expires $expirationRange")
                }
                itemExpired.setOnDebouncedClickListener { expiredTooltip?.show(it) }
            }
        }
    }

    companion object {

        @JvmStatic
        @CheckResult
        private fun setViewColor(
            imageLoader: ImageLoader,
            view: ImageView,
            @DrawableRes drawable: Int,
            loaded: Loaded?,
            isColored: Boolean,
            hasTime: Boolean
        ): Loaded? {
            if (hasTime) {
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
