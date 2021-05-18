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

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.core.FragmentScope
import com.pyamsoft.fridge.entry.EntryListStateModel
import dagger.Binds
import dagger.Module
import javax.inject.Qualifier

@Qualifier @Retention(AnnotationRetention.BINARY) internal annotation class MoveInternalApi

@Module
abstract class ItemMoveModule {

  /** Do this so that both ItemMove view models share the same backing EntryListStateModel */
  @Binds
  @CheckResult
  @FragmentScope
  @MoveInternalApi
  internal abstract fun bindListStateModel(impl: EntryListStateModel): EntryListStateModel
}
