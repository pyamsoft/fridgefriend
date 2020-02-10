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

import android.graphics.drawable.Drawable
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation

internal class TooltipBuilderImpl internal constructor(
    private val builder: Balloon.Builder
) : TooltipBuilder {

    internal var dismissOnClick = false
    internal var dismissOnClickOutside = false

    override fun dismissOnClick(): TooltipBuilder {
        this.dismissOnClick = true
        return this
    }

    override fun dismissOnClickOutside(): TooltipBuilder {
        this.dismissOnClickOutside = true
        return this
    }

    override fun setText(text: String): TooltipBuilder {
        builder.setText(text)
        return this
    }

    override fun setIcon(icon: Drawable): TooltipBuilder {
        builder.setIconDrawable(icon)
        return this
    }

    override fun setIconRes(icon: Int): TooltipBuilder {
        builder.setIconDrawableResource(icon)
        return this
    }

    override fun setBackground(color: Int): TooltipBuilder {
        builder.setBackgroundColor(color)
        return this
    }

    override fun setBackgroundRes(color: Int): TooltipBuilder {
        builder.setBackgroundColorResource(color)
        return this
    }

    override fun setTextColor(color: Int): TooltipBuilder {
        builder.setTextColor(color)
        return this
    }

    override fun setTextColorRes(color: Int): TooltipBuilder {
        builder.setTextColorResource(color)
        return this
    }

    override fun setAnimation(animation: Tooltip.Animation?): TooltipBuilder {
        val anim = if (animation == null) BalloonAnimation.NONE else when (animation) {
            Tooltip.Animation.FADE -> BalloonAnimation.FADE
            Tooltip.Animation.CIRCLE -> BalloonAnimation.CIRCULAR
        }
        builder.setBalloonAnimation(anim)
        return this
    }

    override fun setArrowPosition(value: Float): TooltipBuilder {
        builder.setArrowPosition(value)
        return this
    }

    override fun setTextSize(value: Float): TooltipBuilder {
        builder.setTextSize(value)
        return this
    }
}
