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

package com.pyamsoft.fridge.detail.item

import android.view.ViewGroup
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.detail.create.list.CreationListInteractor
import com.pyamsoft.fridge.detail.item.DetailItemComponent.DetailItemModule
import com.pyamsoft.fridge.detail.item.add.AddNewItemHandler
import com.pyamsoft.fridge.detail.item.add.AddNewItemHandler.AddNewEvent
import com.pyamsoft.fridge.detail.item.add.AddNewItemUiComponent
import com.pyamsoft.fridge.detail.item.add.AddNewItemUiComponentImpl
import com.pyamsoft.fridge.detail.item.add.AddNewItemView
import com.pyamsoft.fridge.detail.item.fridge.DetailItemCallback
import com.pyamsoft.fridge.detail.item.fridge.DetailItemHandler
import com.pyamsoft.fridge.detail.item.fridge.DetailItemHandler.DetailItemEvent
import com.pyamsoft.fridge.detail.item.fridge.DetailListItem
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemDate
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemName
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemPresence
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemStrikethrough
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemUiComponent
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemUiComponentImpl
import com.pyamsoft.fridge.detail.list.DetailListAdapter
import com.pyamsoft.pydroid.arch.UiEventHandler
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Named

@DetailItemScope
@Component(modules = [DetailItemModule::class])
internal interface DetailItemComponent {

  fun inject(holder: DetailListAdapter.AddNewItemViewHolder)

  fun inject(holder: DetailListAdapter.DetailItemViewHolder)

  @Component.Factory
  interface Factory {

    @CheckResult
    fun create(
      @BindsInstance parent: ViewGroup,
      @BindsInstance item: FridgeItem,
      @BindsInstance @Named("item_editable") editable: Boolean,
      @BindsInstance imageLoader: ImageLoader,
      @BindsInstance theming: Theming,
      @BindsInstance interactor: CreationListInteractor,
      @BindsInstance fakeRealtime: EventBus<FridgeItemChangeEvent>
    ): DetailItemComponent

  }

  @Module
  abstract class DetailItemModule {

    @Binds
    @CheckResult
    internal abstract fun bindStrikeItemCallback(impl: DetailItemCallback): DetailListItemStrikethrough.Callback

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

    @Binds
    @CheckResult
    internal abstract fun bindAddNewCallback(impl: AddNewItemHandler): AddNewItemView.Callback

    @Binds
    @CheckResult
    internal abstract fun bindAddNewHandler(impl: AddNewItemHandler): UiEventHandler<AddNewEvent, AddNewItemView.Callback>

    @Binds
    @CheckResult
    internal abstract fun bindAddNewComponent(impl: AddNewItemUiComponentImpl): AddNewItemUiComponent

    @Module
    companion object {

      @Provides
      @JvmStatic
      @DetailItemScope
      fun provideAddBus(): EventBus<AddNewEvent> {
        return RxBus.create()
      }

      @Provides
      @JvmStatic
      @DetailItemScope
      fun provideDetailItemBus(): EventBus<DetailItemEvent> {
        return RxBus.create()
      }
    }

  }

}
