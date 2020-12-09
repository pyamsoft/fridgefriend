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

package com.pyamsoft.fridge.ui

import android.view.View
import androidx.core.content.withStyledAttributes
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.R
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets

fun View.applyToolbarOffset(owner: LifecycleOwner) {
    val view = this
    view.context.withStyledAttributes(
        R.attr.toolbarStyle,
        intArrayOf(R.attr.actionBarSize)
    ) {
        val sizeId = getResourceId(0, 0)
        if (sizeId != 0) {
            val toolbarHeight = view.context.resources.getDimensionPixelSize(sizeId)
            view.doOnApplyWindowInsets(owner) { v, insets, padding ->
                val toolbarTopMargin = padding.top + insets.systemWindowInsetTop
                val newPaddingTop = toolbarHeight + toolbarTopMargin
                v.updatePadding(top = newPaddingTop)
            }
        }
    }
}
