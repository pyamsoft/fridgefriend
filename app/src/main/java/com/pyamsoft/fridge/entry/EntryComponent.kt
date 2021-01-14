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

package com.pyamsoft.fridge.entry

import android.app.Activity
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.pyamsoft.fridge.ThemeProviderModule
import com.pyamsoft.fridge.core.ViewModelFactoryModule
import com.pyamsoft.fridge.tooltip.balloon.TooltipModule
import com.pyamsoft.fridge.ui.appbar.AppBarActivity
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import dagger.*
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Named

@Subcomponent(
    modules = [
        EntryComponent.ComponentModule::class,
        ViewModelFactoryModule::class,
        EntryListModule::class,
        ThemeProviderModule::class,
        TooltipModule::class
    ]
)
internal interface EntryComponent {

    fun inject(fragment: EntryFragment)

    @Subcomponent.Factory
    interface Factory {

        @CheckResult
        fun create(
            @BindsInstance appBarActivity: AppBarActivity,
            @BindsInstance toolbarActivity: ToolbarActivity,
            @BindsInstance activity: Activity,
            @BindsInstance owner: LifecycleOwner,
            @BindsInstance parent: ViewGroup,
        ): EntryComponent
    }

    @Module
    abstract class ComponentModule {

        @Binds
        @IntoMap
        @ClassKey(EntryViewModel::class)
        internal abstract fun bindViewModel(impl: EntryViewModel): ViewModel

        @Module
        companion object {

            @JvmStatic
            @Provides
            @CheckResult
            @Named("is_interactive")
            internal fun provideInteractive(): Boolean {
                return true
            }

        }
    }
}
