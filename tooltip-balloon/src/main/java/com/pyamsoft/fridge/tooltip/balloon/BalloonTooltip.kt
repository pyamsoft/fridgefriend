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

import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.tooltip.Tooltip
import com.skydoves.balloon.Balloon

internal class BalloonTooltip internal constructor(
    creator: LazyBalloon,
    private val direction: Tooltip.Direction,
) : Tooltip {

    private val balloon: Balloon by lazy(LazyThreadSafetyMode.NONE) { creator.create() }

    override fun show(anchor: View) = when (direction) {
        Tooltip.Direction.CENTER -> balloon.show(anchor)
        Tooltip.Direction.TOP -> balloon.showAlignTop(anchor)
        Tooltip.Direction.BOTTOM -> balloon.showAlignBottom(anchor)
        Tooltip.Direction.LEFT -> balloon.showAlignLeft(anchor)
        Tooltip.Direction.RIGHT -> balloon.showAlignRight(anchor)
    }

    override fun show(anchor: View, xOff: Int, yOff: Int) = when (direction) {
        Tooltip.Direction.CENTER -> balloon.show(anchor, xOff, yOff)
        Tooltip.Direction.TOP -> balloon.showAlignTop(anchor, xOff, yOff)
        Tooltip.Direction.BOTTOM -> balloon.showAlignBottom(anchor, xOff, yOff)
        Tooltip.Direction.LEFT -> balloon.showAlignLeft(anchor, xOff, yOff)
        Tooltip.Direction.RIGHT -> balloon.showAlignRight(anchor, xOff, yOff)
    }

    override fun hide() {
        return balloon.dismiss()
    }

    internal class Builder internal constructor(
        private val builder: Balloon.Builder,
    ) : Tooltip.Builder {

        override fun setText(text: String): Tooltip.Builder {
            return makeNew(builder.setText(text))
        }

        override fun setText(text: Int): Tooltip.Builder {
            return makeNew(builder.setTextResource(text))
        }

        override fun setIcon(icon: Drawable): Tooltip.Builder {
            return makeNew(builder.setIconDrawable(icon))
        }

        override fun setIconRes(icon: Int): Tooltip.Builder {
            return makeNew(builder.setIconDrawableResource(icon))
        }

        companion object {

            @JvmStatic
            @CheckResult
            private fun makeNew(builder: Balloon.Builder): Tooltip.Builder {
                return BalloonTooltip.Builder(builder)
            }
        }
    }

}

