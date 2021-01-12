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

package com.pyamsoft.fridge.detail

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewPropertyAnimatorCompat
import com.pyamsoft.fridge.detail.databinding.DetailContainerBinding
import com.pyamsoft.fridge.ui.R
import com.pyamsoft.fridge.ui.animatePopInFromBottom
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.util.asDp
import com.pyamsoft.pydroid.util.darker
import com.pyamsoft.pydroid.util.lighter
import javax.inject.Inject

class DetailContainer @Inject internal constructor(
    private val theming: ThemeProvider,
    parent: ViewGroup,
) : BaseUiView<DetailViewState, DetailViewEvent.ControlledEvents, DetailContainerBinding>(parent) {

    override val viewBinding = DetailContainerBinding::inflate

    override val layoutRoot by boundView { detailContainer }

    private var animator: ViewPropertyAnimatorCompat? = null

    init {
        doOnTeardown {
            animator?.cancel()
            animator = null
        }

        doOnInflate {
            layoutRoot.background = GradientDrawable().apply {
                val context = layoutRoot.context
                shape = GradientDrawable.RECTANGLE
                val radius = 16.asDp(context).toFloat()
                cornerRadii = floatArrayOf(radius, radius, radius, radius, 0F, 0F, 0F, 0F)

                val backgroundColor = context.getColor(R.color.windowBackground)
                val ratio = 0.15F
                val listBackgroundColor = if (theming.isDarkTheme()) {
                    backgroundColor.lighter(ratio)
                } else {
                    backgroundColor.darker(ratio)
                }
                color = ColorStateList.valueOf(listBackgroundColor)
            }
        }

        doOnInflate {
            if (animator == null) {
                animator = animatePopInFromBottom(layoutRoot)
            }
        }
    }

    fun layout(func: ConstraintSet.() -> Unit) {
        return binding.detailContainer.layout(func)
    }

}
