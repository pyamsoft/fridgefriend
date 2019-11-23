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
    private inline fun create(builder: TooltipBuilder.() -> TooltipBuilder): Balloon {
        return createBalloon(context.applicationContext) {
            setArrowSize(10)
            setWidthRatio(1.0f)
            setHeight(65)
            setArrowPosition(0.7f)
            setCornerRadius(4f)
            setAlpha(0.9f)
            setBalloonAnimation(BalloonAnimation.CIRCULAR)
            TooltipBuilderImpl(this).builder()
        }
    }

    override fun center(): Tooltip {
        return center(EMPTY_BUILDER)
    }

    override fun center(builder: TooltipBuilder.() -> TooltipBuilder): Tooltip {
        return TooltipImpl(create(builder), Tooltip.Direction.CENTER)
    }

    override fun top(): Tooltip {
        return top(EMPTY_BUILDER)
    }

    override fun top(builder: TooltipBuilder.() -> TooltipBuilder): Tooltip {
        return TooltipImpl(create(builder), Tooltip.Direction.TOP)
    }

    override fun left(): Tooltip {
        return left(EMPTY_BUILDER)
    }

    override fun left(builder: TooltipBuilder.() -> TooltipBuilder): Tooltip {
        return TooltipImpl(create(builder), Tooltip.Direction.LEFT)
    }

    override fun right(): Tooltip {
        return right(EMPTY_BUILDER)
    }

    override fun right(builder: TooltipBuilder.() -> TooltipBuilder): Tooltip {
        return TooltipImpl(create(builder), Tooltip.Direction.RIGHT)
    }

    override fun bottom(): Tooltip {
        return bottom(EMPTY_BUILDER)
    }

    override fun bottom(builder: TooltipBuilder.() -> TooltipBuilder): Tooltip {
        return TooltipImpl(create(builder), Tooltip.Direction.BOTTOM)
    }

    companion object {

        private val EMPTY_BUILDER: TooltipBuilder.() -> TooltipBuilder = { this }
    }
}
