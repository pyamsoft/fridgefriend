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

package com.pyamsoft.fridge.entry

import com.pyamsoft.fridge.entry.action.EntryActionHandler.ActionEvent
import com.pyamsoft.fridge.entry.list.EntryListHandler.ListEvent
import com.pyamsoft.fridge.entry.toolbar.EntryToolbarHandler.ToolbarEvent
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.RxBus
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object EntrySingletonModule {

  @Provides
  @JvmStatic
  @Singleton
  internal fun provideToolbarEventBus(): EventBus<ToolbarEvent> {
    return RxBus.create()
  }

  @Provides
  @JvmStatic
  @Singleton
  internal fun provideListEventBus(): EventBus<ListEvent> {
    return RxBus.create()
  }

  @Provides
  @JvmStatic
  @Singleton
  internal fun provideActionEventBus(): EventBus<ActionEvent> {
    return RxBus.create()
  }

}
