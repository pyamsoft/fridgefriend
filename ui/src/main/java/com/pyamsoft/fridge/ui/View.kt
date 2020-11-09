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
import com.google.android.material.R
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets

fun View.applyToolbarOffset() {
    this.doOnApplyWindowInsets { v, insets, padding ->
        val toolbarTopMargin = padding.top + insets.systemWindowInsetTop
        v.context.withStyledAttributes(
            R.attr.toolbarStyle,
            intArrayOf(R.attr.actionBarSize)
        ) {
            val sizeId = getResourceId(0, 0)
            if (sizeId != 0) {
                val toolbarHeight = v.context.resources.getDimensionPixelSize(sizeId)
                v.updatePadding(top = toolbarTopMargin + toolbarHeight + v.paddingTop)
            }
        }
    }
}
