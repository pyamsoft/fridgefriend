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
import com.pyamsoft.fridge.detail.list.DetailListAdapter
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named

@DetailItemScope
@Component
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

}
