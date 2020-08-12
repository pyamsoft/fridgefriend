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

package com.pyamsoft.fridge.tooltip

import android.app.Activity
import android.view.View
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject

internal class PopupCreatorImpl @Inject internal constructor(
    activity: Activity
) : TipCreatorImpl<PopupBuilder, Popup>(activity), PopupCreator {

    override fun build(
        owner: LifecycleOwner,
        builder: PopupBuilder.() -> PopupBuilder
    ): BalloonCreator {
        var configureHook: ((Hideable, View) -> Unit)? = null
        val balloonBuilder = newBuilder(owner) {
            val realBuilder = PopupBuilderImpl(this).apply { builder() }
            configureHook = realBuilder.configure
        }

        val params = BaloonParameters(dismissOnClick = false, dismissOnTouchOutside = false)
        return BalloonCreator(balloonBuilder, params, configureHook)
    }

    override fun create(creator: BalloonCreator, direction: TipDirection): Popup {
        return Popup(creator, direction)
    }
}
