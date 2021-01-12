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

package com.pyamsoft.fridge.detail.expand.move

import android.app.Activity
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.ThemeProviderModule
import com.pyamsoft.fridge.core.FragmentScope
import com.pyamsoft.fridge.core.FridgeViewModelFactory
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.entry.EntryListModule
import com.pyamsoft.fridge.tooltip.balloon.TooltipModule
import com.pyamsoft.fridge.ui.appbar.AppBarActivity
import com.pyamsoft.pydroid.arch.UiViewModel
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Named

@FragmentScope
@Subcomponent(modules = [
    ItemMoveComponent.ViewModelModule::class,
    ItemMoveModule::class,
    EntryListModule::class,
    ThemeProviderModule::class,
    TooltipModule::class
])
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
    abstract class ViewModelModule {

        @Binds
        internal abstract fun bindViewModelFactory(factory: FridgeViewModelFactory): ViewModelProvider.Factory

        @Binds
        @IntoMap
        @ClassKey(ItemMoveViewModel::class)
        internal abstract fun itemMoveViewModel(viewModel: ItemMoveViewModel): UiViewModel<*, *, *>

        @Binds
        @IntoMap
        @ClassKey(ItemMoveListViewModel::class)
        internal abstract fun itemMoveListViewModel(viewModel: ItemMoveListViewModel): UiViewModel<*, *, *>

        @Module
        companion object {

            @JvmStatic
            @Provides
            @CheckResult
            @Named("is_interactive")
            internal fun provideInteractive(): Boolean {
                return false
            }

        }
    }
}
