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
 */

package com.pyamsoft.fridge.tooltip.balloon

import android.graphics.drawable.Drawable
import com.pyamsoft.fridge.tooltip.TooltipBuilder
import com.skydoves.balloon.Balloon

internal class BalloonTooltipBuilder internal constructor(
    private val builder: Balloon.Builder
) : TooltipBuilder {

    override fun setText(text: String): TooltipBuilder {
        builder.setText(text)
        return this
    }

    override fun setText(text: Int): TooltipBuilder {
        builder.setTextResource(text)
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
}
