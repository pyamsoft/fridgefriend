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

package com.pyamsoft.fridge.core.view

import android.graphics.Color
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils

@ColorInt
@CheckResult
fun @receiver:ColorInt Int.darker(ratio: Float): Int {
    return blend(Color.BLACK, ratio)
}

@ColorInt
@CheckResult
fun @receiver:ColorInt Int.lighter(ratio: Float): Int {
    return blend(Color.WHITE, ratio)
}

@ColorInt
@CheckResult
fun @receiver:ColorInt Int.blend(@ColorInt color: Int, ratio: Float): Int {
    return ColorUtils.blendARGB(this, color, ratio)
}
