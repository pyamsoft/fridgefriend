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

import android.view.View
import com.pyamsoft.fridge.tooltip.TipDirection
import com.pyamsoft.fridge.tooltip.Tooltip
import com.skydoves.balloon.Balloon

internal class BalloonTooltip internal constructor(
    creator: LazyBalloon,
    private val direction: TipDirection,
) : Tooltip {

    private val balloon: Balloon by lazy(LazyThreadSafetyMode.NONE) { creator.create() }

    override fun show(anchor: View) {
        return when (direction) {
            TipDirection.CENTER -> balloon.show(anchor)
            TipDirection.TOP -> balloon.showAlignTop(anchor)
            TipDirection.BOTTOM -> balloon.showAlignBottom(anchor)
            TipDirection.LEFT -> balloon.showAlignLeft(anchor)
            TipDirection.RIGHT -> balloon.showAlignRight(anchor)
        }
    }

    override fun show(anchor: View, xOff: Int, yOff: Int) {
        return when (direction) {
            TipDirection.CENTER -> balloon.show(anchor, xOff, yOff)
            TipDirection.TOP -> balloon.showAlignTop(anchor, xOff, yOff)
            TipDirection.BOTTOM -> balloon.showAlignBottom(anchor, xOff, yOff)
            TipDirection.LEFT -> balloon.showAlignLeft(anchor, xOff, yOff)
            TipDirection.RIGHT -> balloon.showAlignRight(anchor, xOff, yOff)
        }
    }

    override fun hide() {
        return balloon.dismiss()
    }

}

