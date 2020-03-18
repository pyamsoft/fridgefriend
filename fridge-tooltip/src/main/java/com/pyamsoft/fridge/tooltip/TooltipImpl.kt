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

internal class TooltipImpl internal constructor(
    composite: BalloonAndBuilder,
    direction: TipDirection
) : Tooltip(composite, direction) {

    private val delegate: Tip = TipImpl(composite.balloon, direction)

    override fun isShowing(): Boolean {
        return delegate.isShowing()
    }

    override fun show(anchor: View) {
        return delegate.show(anchor)
    }

    override fun show(anchor: View, xOff: Int, yOff: Int) {
        return delegate.show(anchor, xOff, yOff)
    }

    override fun hide() {
        return delegate.hide()
    }
}
