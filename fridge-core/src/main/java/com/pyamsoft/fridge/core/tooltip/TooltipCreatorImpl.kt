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

package com.pyamsoft.fridge.core.tooltip

import android.content.Context
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.createBalloon
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TooltipCreatorImpl @Inject internal constructor(
    private val context: Context
) : TooltipCreator {

    @CheckResult
    private fun create(
        owner: LifecycleOwner,
        builder: TooltipBuilder.() -> TooltipBuilder
    ): Balloon {
        return createBalloon(context.applicationContext) {
            setArrowSize(10)
            setWidthRatio(1.0f)
            setHeight(65)
            setArrowPosition(0.7f)
            setCornerRadius(4f)
            setAlpha(0.9f)
            setBalloonAnimation(BalloonAnimation.FADE)
            setLifecycleOwner(owner)
            TooltipBuilderImpl(this).builder()
        }
    }

    override fun center(owner: LifecycleOwner): Tooltip {
        return center(owner, EMPTY_BUILDER)
    }

    override fun center(
        owner: LifecycleOwner,
        builder: TooltipBuilder.() -> TooltipBuilder
    ): Tooltip {
        return TooltipImpl(create(owner, builder), Tooltip.Direction.CENTER)
    }

    override fun top(owner: LifecycleOwner): Tooltip {
        return top(owner, EMPTY_BUILDER)
    }

    override fun top(owner: LifecycleOwner, builder: TooltipBuilder.() -> TooltipBuilder): Tooltip {
        return TooltipImpl(create(owner, builder), Tooltip.Direction.TOP)
    }

    override fun left(owner: LifecycleOwner): Tooltip {
        return left(owner, EMPTY_BUILDER)
    }

    override fun left(
        owner: LifecycleOwner,
        builder: TooltipBuilder.() -> TooltipBuilder
    ): Tooltip {
        return TooltipImpl(create(owner, builder), Tooltip.Direction.LEFT)
    }

    override fun right(owner: LifecycleOwner): Tooltip {
        return right(owner, EMPTY_BUILDER)
    }

    override fun right(
        owner: LifecycleOwner,
        builder: TooltipBuilder.() -> TooltipBuilder
    ): Tooltip {
        return TooltipImpl(create(owner, builder), Tooltip.Direction.RIGHT)
    }

    override fun bottom(owner: LifecycleOwner): Tooltip {
        return bottom(owner, EMPTY_BUILDER)
    }

    override fun bottom(
        owner: LifecycleOwner,
        builder: TooltipBuilder.() -> TooltipBuilder
    ): Tooltip {
        return TooltipImpl(create(owner, builder), Tooltip.Direction.BOTTOM)
    }

    companion object {

        private val EMPTY_BUILDER: TooltipBuilder.() -> TooltipBuilder = { this }
    }
}
