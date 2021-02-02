/*
 * Copyright 2021 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.tooltip.balloon

import android.app.Activity
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.tooltip.R
import com.pyamsoft.fridge.tooltip.Tooltip
import com.pyamsoft.fridge.tooltip.TooltipCreator
import com.pyamsoft.fridge.tooltip.TooltipParameters
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import javax.inject.Inject

internal class BalloonTooltipCreator @Inject internal constructor(
    private val activity: Activity,
) : TooltipCreator {

    @CheckResult
    private fun newBuilder(withBuilder: Balloon.Builder.() -> Unit): Balloon.Builder {
        return Balloon.Builder(activity).apply {
            setIsVisibleArrow(false)
            setCornerRadius(16F)
            setAlpha(0.85F)
            setBackgroundColorResource(R.color.tooltipBackground)
            setTextColorResource(R.color.tooltipText)
            setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            setArrowPosition(0.5F)
            setTextSize(14F)

            val padding = 4
            setPaddingTop(padding)
            setPaddingBottom(padding)
            setPaddingLeft(padding * 2)
            setPaddingRight(padding * 2)

            withBuilder(this)
        }
    }

    @CheckResult
    private fun makeBalloon(builder: Balloon.Builder, params: TooltipParameters): Balloon {
        return builder
            .setDismissWhenClicked(params.dismissOnClick)
            .setDismissWhenTouchOutside(params.dismissOnTouchOutside)
            .build()
    }

    override fun top(): Tooltip {
        return top(EMPTY_BUILDER)
    }

    override fun top(builder: Tooltip.Builder.() -> Tooltip.Builder): Tooltip {
        return create(build(builder), Tooltip.Direction.TOP)
    }

    private fun build(builder: Tooltip.Builder.() -> Tooltip.Builder): LazyBalloon {
        val balloonBuilder = newBuilder {
            setHeight(65)
            BalloonTooltip.Builder(this).apply { builder() }
        }

        val params = TooltipParameters(dismissOnClick = true, dismissOnTouchOutside = true)
        return LazyBalloon { makeBalloon(balloonBuilder, params) }
    }

    companion object {

        private val EMPTY_BUILDER: Tooltip.Builder.() -> Tooltip.Builder = { this }

        @JvmStatic
        @CheckResult
        private fun create(balloon: LazyBalloon, direction: Tooltip.Direction): BalloonTooltip {
            return BalloonTooltip(balloon, direction)
        }
    }

}
