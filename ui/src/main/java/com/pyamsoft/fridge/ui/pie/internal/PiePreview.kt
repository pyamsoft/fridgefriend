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

import android.graphics.Color
import com.pyamsoft.fridge.ui.pie.PieData

internal object PiePreview {

    val PREVIEW_DATA = listOf(
        PieData.Part(10F.asData(), Color.parseColor("#E57373").asColor()),
        PieData.Part(20F.asData(), Color.parseColor("#29B6F6").asColor()),
        PieData.Part(60F.asData(), Color.parseColor("#9CCC65").asColor()),
        PieData.Part(41F.asData(), Color.parseColor("#78909C").asColor()),
        PieData.Part(100F.asData(), Color.parseColor("#FFCA28").asColor())
    )

}