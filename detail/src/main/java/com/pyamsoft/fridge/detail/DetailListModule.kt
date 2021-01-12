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

package com.pyamsoft.fridge.detail

import android.content.Context
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.core.FragmentScope
import com.pyamsoft.fridge.detail.item.DetailItemComponent
import com.pyamsoft.fridge.tooltip.TooltipCreator
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class DetailInternalApi

@Module
abstract class DetailListModule {

    /**
     * Do this so that both Detail view models share the same backing DetailListStateModel
     */
    @Binds
    @CheckResult
    @FragmentScope
    @DetailInternalApi
    internal abstract fun bindListStateModel(impl: DetailListStateModel): DetailListStateModel

    @Module
    companion object {

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideDetailListItemComponentCreator(
            context: Context,
            tooltipCreator: TooltipCreator,
            theming: ThemeProvider
        ): DetailItemComponent.Factory {
            return Injector.obtainFromApplication<DetailListComponent.Factory>(context)
                .create(tooltipCreator, theming).plusItemComponent()
        }
    }
}
