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
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeViewModelFactory
import com.pyamsoft.fridge.ViewModelKey
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.detail.ExpandComponent.ViewModelModule
import com.pyamsoft.fridge.detail.expand.ExpandItemViewModel
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import dagger.multibindings.IntoMap

@Subcomponent(modules = [ViewModelModule::class])
internal interface ExpandComponent {

    fun inject(fragment: ExpandedFragment)

    @Subcomponent.Factory
    interface Factory {

        @CheckResult
        fun create(
            @BindsInstance theming: ThemeProvider,
            @BindsInstance parent: ViewGroup,
            @BindsInstance item: FridgeItem,
            @BindsInstance entry: FridgeEntry,
            @BindsInstance defaultPresence: Presence
        ): ExpandComponent
    }

    @Module
    abstract class ViewModelModule {

        @Binds
        internal abstract fun bindViewModelFactory(factory: FridgeViewModelFactory): ViewModelProvider.Factory

        @Binds
        @IntoMap
        @ViewModelKey(ExpandItemViewModel::class)
        internal abstract fun expandViewModel(viewModel: ExpandItemViewModel): UiViewModel<*, *, *>
    }
}
