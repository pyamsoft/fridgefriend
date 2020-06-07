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

package com.pyamsoft.fridge.main

import android.view.View
import androidx.core.content.ContextCompat
import com.pyamsoft.pydroid.util.toDp

internal class MainNavBackground internal constructor(
    view: View,
    location: Int
) : BarBackground(
    ContextCompat.getColor(view.context, R.color.colorPrimary)
) {

    init {
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    private val location: Float = location.toDp(view.context).toFloat()

    private val radius = (view.height - 4.toDp(view.context)).toFloat()

    override fun holePunch(): HolePunch? {
        return HolePunch(location, radius)
    }
}
