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

package com.pyamsoft.fridge.entry.create

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.ViewModel
import com.pyamsoft.fridge.ThemeProviderModule
import com.pyamsoft.fridge.core.ViewModelFactoryModule
import com.pyamsoft.fridge.tooltip.balloon.TooltipModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Subcomponent(
    modules = [
        CreateEntryComponent.ComponentModule::class,
        ViewModelFactoryModule::class,
        ThemeProviderModule::class,
        TooltipModule::class
    ]
)
internal interface CreateEntryComponent {

    fun inject(sheet: CreateEntrySheet)

    @Subcomponent.Factory
    interface Factory {

        @CheckResult
        fun create(@BindsInstance parent: ViewGroup): CreateEntryComponent
    }

    @Module
    abstract class ComponentModule {

        @Binds
        @IntoMap
        @ClassKey(CreateEntryViewModel::class)
        internal abstract fun bindViewModel(impl: CreateEntryViewModel): ViewModel
    }

}
