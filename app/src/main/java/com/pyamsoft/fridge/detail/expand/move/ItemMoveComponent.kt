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

package com.pyamsoft.fridge.detail.expand.move

import android.app.Activity
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.pyamsoft.fridge.core.FragmentScope
import com.pyamsoft.fridge.core.ViewModelFactoryModule
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.tooltip.balloon.TooltipModule
import com.pyamsoft.fridge.ui.ThemeProviderModule
import com.pyamsoft.pydroid.ui.app.AppBarActivity
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@FragmentScope
@Subcomponent(
    modules =
        [
            ItemMoveComponent.ComponentModule::class,
            ViewModelFactoryModule::class,
            ItemMoveModule::class,
            ThemeProviderModule::class,
            TooltipModule::class])
internal interface ItemMoveComponent {

  fun inject(dialog: ItemMoveDialog)

  @Subcomponent.Factory
  interface Factory {

    @CheckResult
    fun create(
        @BindsInstance appBarActivity: AppBarActivity,
        @BindsInstance activity: Activity,
        @BindsInstance owner: LifecycleOwner,
        @BindsInstance parent: ViewGroup,
        @BindsInstance itemId: FridgeItem.Id,
        @BindsInstance itemEntryId: FridgeEntry.Id,
    ): ItemMoveComponent
  }

  @Module
  abstract class ComponentModule {

    @Binds
    @IntoMap
    @ClassKey(ItemMoveViewModel::class)
    internal abstract fun bindViewModel(impl: ItemMoveViewModel): ViewModel

    @Binds
    @IntoMap
    @ClassKey(ItemMoveListViewModel::class)
    internal abstract fun bindListViewModel(impl: ItemMoveListViewModel): ViewModel
  }
}
