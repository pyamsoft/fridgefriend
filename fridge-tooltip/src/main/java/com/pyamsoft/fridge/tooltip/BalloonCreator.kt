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
import com.skydoves.balloon.Balloon

internal data class BalloonCreator internal constructor(
    private val builder: Balloon.Builder,
    private val params: BaloonParameters,
    private val configure: ((Hideable, View) -> Unit)?
) {

    @CheckResult
    fun create(): Balloon {
        return builder
            .setDismissWhenClicked(params.dismissOnClick)
            .setDismissWhenTouchOutside(params.dismissOnTouchOutside)
            .build().also { b ->
                configure?.let { hook ->
                    val view = b.getContentView()
                    val hideable = object : Hideable {
                        override fun hide() {
                            b.dismiss()
                        }
                    }
                    hook(hideable, view)
                }
            }
    }
}
