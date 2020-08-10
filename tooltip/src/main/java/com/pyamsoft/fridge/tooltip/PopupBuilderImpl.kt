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
import com.skydoves.balloon.Balloon

internal class PopupBuilderImpl internal constructor(
    private val builder: Balloon.Builder
) : PopupBuilder {

    internal var configure: ((Hideable, View) -> Unit)? = null

    override fun setLayout(layout: Int): PopupBuilder {
        builder.setLayout(layout)
        return this
    }

    override fun configure(configure: (hideable: Hideable, view: View) -> Unit): PopupBuilder {
        this.configure = configure
        return this
    }
}
