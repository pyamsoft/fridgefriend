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
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailItemViewHolder
import com.pyamsoft.fridge.detail.item.DetailItemComponent.ViewModelModule
import com.pyamsoft.pydroid.arch.UiViewModel
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import dagger.multibindings.IntoMap
import javax.inject.Named

@DetailItemScope
@Subcomponent(modules = [ViewModelModule::class])
internal interface DetailItemComponent {

    fun inject(holder: DetailItemViewHolder)

    @Subcomponent.Factory
    interface Factory {

        @CheckResult
        fun create(
            @BindsInstance parent: ViewGroup,
            @BindsInstance item: FridgeItem,
            @BindsInstance @Named("item_editable") editable: Boolean
        ): DetailItemComponent
    }

    @Module
    abstract class ViewModelModule {

        @Binds
        internal abstract fun bindViewModelFactory(factory: DetailItemViewModelFactory): ViewModelProvider.Factory

        @Binds
        @IntoMap
        @ViewModelKey(DetailListItemViewModel::class)
        internal abstract fun detailListItemViewModel(viewModel: DetailListItemViewModel): UiViewModel<*, *, *>
    }
}
