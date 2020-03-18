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
import androidx.annotation.CheckResult
import androidx.annotation.LayoutRes
import com.skydoves.balloon.Balloon

internal class PopupImpl internal constructor(
    composite: BalloonAndBuilder,
    direction: TipDirection,
    @LayoutRes layout: Int,
    configure: (hide: Hideable, view: View) -> Unit
) : Popup {

    private val delegate: Tip

    init {
        val builder = newBuilder(composite.builder, layout)
        val rawLazyBalloon = BalloonAndBuilder(builder, composite.params).balloon
        val lazyBalloon = lazy(LazyThreadSafetyMode.NONE) {
            val balloon = rawLazyBalloon.value
            val hideable = object : Hideable {
                override fun hide() {
                    balloon.dismiss()
                }
            }

            val contentView = balloon.getContentView()
            configure(hideable, contentView)

            balloon
        }
        delegate = TipImpl(lazyBalloon, direction)
    }

    @CheckResult
    private fun newBuilder(builder: Balloon.Builder, @LayoutRes layout: Int): Balloon.Builder {
        return builder.apply {
            setLayout(layout)
        }
    }

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
