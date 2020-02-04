/*
 * Copyright 2019 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.detail

import android.view.ViewGroup
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.core.tooltip.TooltipCreator
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.detail.expand.date.DateSelectPayload
import com.pyamsoft.fridge.detail.item.DetailItemComponent
import com.pyamsoft.pydroid.arch.EventBus
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import dagger.Module
import dagger.Provides

@Module
abstract class DetailListModule {

    @Module
    companion object {

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideDetailListItemComponentCreator(
            interactor: DetailInteractor,
            tooltipCreator: TooltipCreator,
            imageLoader: ImageLoader,
            theming: ThemeProvider,
            realtime: FridgeItemRealtime,
            fakeRealtime: EventBus<FridgeItemChangeEvent>,
            dateSelectBus: EventBus<DateSelectPayload>,
            listItemPresence: FridgeItem.Presence
        ): DetailItemComponentCreator {
            val component = DaggerDetailListComponent.factory()
                .create(
                    imageLoader, theming, interactor,
                    realtime, fakeRealtime, dateSelectBus,
                    listItemPresence
                ).plusItemComponent()

            return object :
                DetailItemComponentCreator {

                override fun create(parent: ViewGroup, editable: Boolean): DetailItemComponent {
                    return component.create(tooltipCreator, parent, editable)
                }
            }
        }
    }
}
