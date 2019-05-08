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

package com.pyamsoft.fridge.detail.expand

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.detail.item.fridge.DetailItemCallback
import com.pyamsoft.fridge.detail.item.fridge.DetailItemHandler
import com.pyamsoft.fridge.detail.item.fridge.DetailItemHandler.DetailItemEvent
import com.pyamsoft.fridge.detail.item.fridge.DetailListItem
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemDate
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemName
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemPresence
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemUiComponent
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemUiComponentImpl
import com.pyamsoft.pydroid.arch.UiEventHandler
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.RxBus
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
abstract class ExpandModule {

  @Binds
  @CheckResult
  internal abstract fun bindUiComponent(impl: ExpandUiComponentImpl): ExpandUiComponent

  @Binds
  @CheckResult
  internal abstract fun bindNameItemCallback(impl: DetailItemCallback): DetailListItemName.Callback

  @Binds
  @CheckResult
  internal abstract fun bindDateItemCallback(impl: DetailItemCallback): DetailListItemDate.Callback

  @Binds
  @CheckResult
  internal abstract fun bindPresenceItemCallback(impl: DetailItemCallback): DetailListItemPresence.Callback

  @Binds
  @CheckResult
  internal abstract fun bindBaseItemCallback(impl: DetailItemCallback): DetailListItem.Callback

  @Binds
  @CheckResult
  internal abstract fun bindItemCallback(impl: DetailItemHandler): DetailItemCallback

  @Binds
  @CheckResult
  internal abstract fun bindItemHandler(impl: DetailItemHandler): UiEventHandler<DetailItemEvent, DetailItemCallback>

  @Binds
  @CheckResult
  internal abstract fun bindDetailComponent(impl: DetailListItemUiComponentImpl): DetailListItemUiComponent

  @Module
  companion object {

    @Provides
    @JvmStatic
    @ExpandScope
    internal fun provideDetailItemBus(): EventBus<DetailItemEvent> {
      return RxBus.create()
    }

    @Provides
    @JvmStatic
    @ExpandScope
    @Named("item_editable")
    fun provideEditable(): Boolean {
      return true
    }
  }

}
