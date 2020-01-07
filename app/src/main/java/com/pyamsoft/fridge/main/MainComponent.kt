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

package com.pyamsoft.fridge.main

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeViewModelFactory
import com.pyamsoft.fridge.ViewModelKey
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.ui.app.ToolbarActivityProvider
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import dagger.multibindings.IntoMap

@Subcomponent(modules = [MainComponent.ViewModelModule::class])
internal interface MainComponent {

    fun inject(activity: MainActivity)

    @Subcomponent.Factory
    interface Factory {

        @CheckResult
        fun create(
            @BindsInstance parent: ViewGroup,
            @BindsInstance page: MainPage,
            @BindsInstance provider: ToolbarActivityProvider,
            @BindsInstance themeProvider: ThemeProvider
        ): MainComponent
    }

    @Module
    abstract class ViewModelModule {

        @Binds
        internal abstract fun bindViewModelFactory(factory: FridgeViewModelFactory): ViewModelProvider.Factory

        @Binds
        @IntoMap
        @ViewModelKey(MainViewModel::class)
        internal abstract fun entryViewModel(viewModel: MainViewModel): UiViewModel<*, *, *>
    }
}
