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

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.core.FragmentScope
import com.pyamsoft.fridge.entry.EntryInteractor
import com.pyamsoft.fridge.entry.EntryListStateModel
import com.pyamsoft.fridge.ui.BottomOffset
import com.pyamsoft.pydroid.bus.EventBus
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class InternalApi

@Module
abstract class ItemMoveModule {

    @Module
    companion object {

        @JvmStatic
        @Provides
        @CheckResult
        @InternalApi
        @FragmentScope
        internal fun provideListStateModel(
            interactor: EntryInteractor,
            bottomOffsetBus: EventBus<BottomOffset>,
        ): EntryListStateModel {
            return EntryListStateModel(interactor, bottomOffsetBus)
        }
    }
}