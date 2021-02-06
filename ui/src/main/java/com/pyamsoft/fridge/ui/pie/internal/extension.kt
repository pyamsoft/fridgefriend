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

package com.pyamsoft.fridge.ui.pie.internal

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.pyamsoft.fridge.ui.pie.PieData

@Px
@CheckResult
internal fun Context.toPx(@Dp dp: Int): Float = dp * resources.displayMetrics.density

@Px
@CheckResult
internal fun Context.toPx(@Sp sp: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)

@ColorInt
@CheckResult
internal fun Context.getAttributeColor(@AttrRes attrRes: Int): Int {
    val typedValue = TypedValue()
    this.theme.resolveAttribute(attrRes, typedValue, true)
    return typedValue.data
}

@CheckResult
fun @receiver:ColorInt Int.asColor(): PieData.Color {
    return PieData.Color(this)
}

@CheckResult
fun Float.asData(): PieData.Data {
    return PieData.Data(this)
}
