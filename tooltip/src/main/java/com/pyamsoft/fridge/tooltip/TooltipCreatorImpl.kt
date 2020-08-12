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
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject

internal class TooltipCreatorImpl @Inject internal constructor(
    activity: Activity
) : TipCreatorImpl<TooltipBuilder, Tooltip>(activity), TooltipCreator {

    override fun build(
        owner: LifecycleOwner,
        builder: TooltipBuilder.() -> TooltipBuilder
    ): BalloonCreator {
        val balloonBuilder = newBuilder(owner) {
            setHeight(65)
            TooltipBuilderImpl(this).apply { builder() }
        }

        val params = BaloonParameters(dismissOnClick = true, dismissOnTouchOutside = true)
        return BalloonCreator(balloonBuilder, params, null)
    }

    override fun create(creator: BalloonCreator, direction: TipDirection): Tooltip {
        return Tooltip(creator, direction)
    }
}
