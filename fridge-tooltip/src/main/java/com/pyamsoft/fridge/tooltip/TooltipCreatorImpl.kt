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

package com.pyamsoft.fridge.tooltip

import android.app.Activity
import android.view.MotionEvent
import android.view.View
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.core.R
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.OnBalloonClickListener
import com.skydoves.balloon.OnBalloonOutsideTouchListener
import com.skydoves.balloon.createBalloon
import javax.inject.Inject

internal class TooltipCreatorImpl @Inject internal constructor(
    private val activity: Activity
) : TooltipCreator {

    @CheckResult
    private inline fun create(builder: TooltipBuilder.() -> TooltipBuilder): Balloon {
        var dismissOnClick = false
        var dismissOnClickOutside = false

        val balloon = createBalloon(activity) {
            setArrowSize(12)
            setWidthRatio(0.85F)
            setHeight(65)
            setCornerRadius(16F)
            setAlpha(0.85F)
            setBackgroundColorResource(R.color.tooltipBackground)
            setTextColorResource(R.color.tooltipText)

            val tooltipBuilder = TooltipBuilderImpl(
                this
            ).apply {
                setArrowPosition(0.5F)
                setAnimation(Tooltip.Animation.FADE)
                setTextSize(16F)
                builder()
            }

            dismissOnClick = tooltipBuilder.dismissOnClick
            dismissOnClickOutside = tooltipBuilder.dismissOnClickOutside
        }

        if (dismissOnClick) {
            balloon.onBalloonClickListener = object : OnBalloonClickListener {
                override fun onBalloonClick(view: View) {
                    balloon.dismiss()
                }
            }
        }

        if (dismissOnClickOutside) {
            balloon.onBalloonOutsideTouchListener = object : OnBalloonOutsideTouchListener {
                override fun onBalloonOutsideTouch(view: View, event: MotionEvent) {
                    balloon.dismiss()
                }
            }
        }

        return balloon
    }

    override fun center(): Tooltip {
        return center(EMPTY_BUILDER)
    }

    override fun center(builder: TooltipBuilder.() -> TooltipBuilder): Tooltip {
        return TooltipImpl(
            create(builder),
            Tooltip.Direction.CENTER
        )
    }

    override fun top(): Tooltip {
        return top(EMPTY_BUILDER)
    }

    override fun top(builder: TooltipBuilder.() -> TooltipBuilder): Tooltip {
        return TooltipImpl(
            create(builder),
            Tooltip.Direction.TOP
        )
    }

    override fun left(): Tooltip {
        return left(EMPTY_BUILDER)
    }

    override fun left(builder: TooltipBuilder.() -> TooltipBuilder): Tooltip {
        return TooltipImpl(
            create(builder),
            Tooltip.Direction.LEFT
        )
    }

    override fun right(): Tooltip {
        return right(EMPTY_BUILDER)
    }

    override fun right(builder: TooltipBuilder.() -> TooltipBuilder): Tooltip {
        return TooltipImpl(
            create(builder),
            Tooltip.Direction.RIGHT
        )
    }

    override fun bottom(): Tooltip {
        return bottom(EMPTY_BUILDER)
    }

    override fun bottom(builder: TooltipBuilder.() -> TooltipBuilder): Tooltip {
        return TooltipImpl(
            create(builder),
            Tooltip.Direction.BOTTOM
        )
    }

    companion object {

        private val EMPTY_BUILDER: TooltipBuilder.() -> TooltipBuilder = { this }
    }
}
