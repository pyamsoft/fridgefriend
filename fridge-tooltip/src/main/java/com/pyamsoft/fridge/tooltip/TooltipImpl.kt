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

import android.view.View
import com.skydoves.balloon.Balloon

internal class TooltipImpl internal constructor(
    private val balloon: Balloon,
    private val direction: Tooltip.Direction
) : Tooltip {

    override fun isShowing(): Boolean {
        return balloon.isShowing
    }

    override fun show(anchor: View) {
        return when (direction) {
            Tooltip.Direction.CENTER -> balloon.show(anchor)
            Tooltip.Direction.TOP -> balloon.showAlignTop(anchor)
            Tooltip.Direction.BOTTOM -> balloon.showAlignBottom(anchor)
            Tooltip.Direction.LEFT -> balloon.showAlignLeft(anchor)
            Tooltip.Direction.RIGHT -> balloon.showAlignRight(anchor)
        }
    }

    override fun show(anchor: View, xOff: Int, yOff: Int) {
        return when (direction) {
            Tooltip.Direction.CENTER -> balloon.show(anchor, xOff, yOff)
            Tooltip.Direction.TOP -> balloon.showAlignTop(anchor, xOff, yOff)
            Tooltip.Direction.BOTTOM -> balloon.showAlignBottom(anchor, xOff, yOff)
            Tooltip.Direction.LEFT -> balloon.showAlignLeft(anchor, xOff, yOff)
            Tooltip.Direction.RIGHT -> balloon.showAlignRight(anchor, xOff, yOff)
        }
    }

    override fun hide() {
        return balloon.dismiss()
    }
}
