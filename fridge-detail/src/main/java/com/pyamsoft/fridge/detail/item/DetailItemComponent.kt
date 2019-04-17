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
import com.pyamsoft.fridge.detail.item.add.AddNewItemBinder
import com.pyamsoft.fridge.detail.item.add.AddNewItemUiComponent
import com.pyamsoft.fridge.detail.item.add.AddNewItemUiComponentImpl
import com.pyamsoft.fridge.detail.item.add.AddNewItemView
import com.pyamsoft.fridge.detail.item.add.AddNewListItemController
import com.pyamsoft.fridge.detail.item.fridge.DetailItemPresenter
import com.pyamsoft.fridge.detail.item.fridge.DetailListItem
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemController
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemDate
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemName
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemPresence
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemStrikethrough
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemUiComponent
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemUiComponentImpl
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import javax.inject.Named

@DetailItemScope
@Component(modules = [DetailItemModule::class])
internal interface DetailItemComponent {

  fun inject(holder: AddNewListItemController.ViewHolder)

  fun inject(holder: DetailListItemController.ViewHolder)

  @Component.Factory
  interface Factory {

    @CheckResult
    fun create(
      @BindsInstance parent: ViewGroup,
      @BindsInstance item: FridgeItem,
      @BindsInstance @Named("item_editable") editable: Boolean,
      @BindsInstance stateMap: MutableMap<String, Int>,
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
    internal abstract fun bindStrikeItemCallback(impl: DetailItemPresenter): DetailListItemStrikethrough.Callback

    @Binds
    @CheckResult
    internal abstract fun bindNameItemCallback(impl: DetailItemPresenter): DetailListItemName.Callback

    @Binds
    @CheckResult
    internal abstract fun bindDateItemCallback(impl: DetailItemPresenter): DetailListItemDate.Callback

    @Binds
    @CheckResult
    internal abstract fun bindPresenceItemCallback(impl: DetailItemPresenter): DetailListItemPresence.Callback

    @Binds
    @CheckResult
    internal abstract fun bindBaseItemCallback(impl: DetailItemPresenter): DetailListItem.Callback

    @Binds
    @CheckResult
    internal abstract fun bindDetailComponent(impl: DetailListItemUiComponentImpl): DetailListItemUiComponent

    @Binds
    @CheckResult
    internal abstract fun bindAddNewCallback(impl: AddNewItemBinder): AddNewItemView.Callback

    @Binds
    @CheckResult
    internal abstract fun bindAddNewComponent(impl: AddNewItemUiComponentImpl): AddNewItemUiComponent

  }

}