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
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import javax.inject.Inject

internal class TooltipCreatorImpl @Inject internal constructor(
    private val activity: Activity
) : TooltipCreator {

    @CheckResult
    private inline fun create(
        owner: LifecycleOwner,
        crossinline builder: TooltipBuilder.() -> TooltipBuilder
    ): BaloonCreator {
        val balloonBuilder = Balloon.Builder(activity).apply {
            setLifecycleOwner(owner)
            setArrowVisible(false)
            setHeight(65)
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

            TooltipBuilderImpl(this).apply { builder() }
        }

        val params = BaloonParameters(dismissOnClick = true)
        return BaloonCreator(balloonBuilder, params)
    }

    override fun center(owner: LifecycleOwner): Tooltip {
        return center(owner, EMPTY_BUILDER)
    }

    override fun center(
        owner: LifecycleOwner,
        builder: TooltipBuilder.() -> TooltipBuilder
    ): Tooltip {
        return Tooltip(create(owner, builder), TipDirection.CENTER)
    }

    override fun top(owner: LifecycleOwner): Tooltip {
        return top(owner, EMPTY_BUILDER)
    }

    override fun top(owner: LifecycleOwner, builder: TooltipBuilder.() -> TooltipBuilder): Tooltip {
        return Tooltip(create(owner, builder), TipDirection.TOP)
    }

    override fun left(owner: LifecycleOwner): Tooltip {
        return left(owner, EMPTY_BUILDER)
    }

    override fun left(
        owner: LifecycleOwner,
        builder: TooltipBuilder.() -> TooltipBuilder
    ): Tooltip {
        return Tooltip(create(owner, builder), TipDirection.LEFT)
    }

    override fun right(owner: LifecycleOwner): Tooltip {
        return right(owner, EMPTY_BUILDER)
    }

    override fun right(
        owner: LifecycleOwner,
        builder: TooltipBuilder.() -> TooltipBuilder
    ): Tooltip {
        return Tooltip(create(owner, builder), TipDirection.RIGHT)
    }

    override fun bottom(owner: LifecycleOwner): Tooltip {
        return bottom(owner, EMPTY_BUILDER)
    }

    override fun bottom(
        owner: LifecycleOwner,
        builder: TooltipBuilder.() -> TooltipBuilder
    ): Tooltip {
        return Tooltip(create(owner, builder), TipDirection.BOTTOM)
    }

    companion object {

        private val EMPTY_BUILDER: TooltipBuilder.() -> TooltipBuilder = { this }
    }
}
