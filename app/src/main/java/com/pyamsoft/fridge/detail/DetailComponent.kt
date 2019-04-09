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
import com.pyamsoft.fridge.detail.DetailComponent.DetailProvider
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@DetailScope
@Subcomponent(modules = [DetailProvider::class])
internal interface DetailComponent {

  @CheckResult
  fun plusCreationComponent(): CreationComponent.Builder

  @CheckResult
  fun plusShoppingComponent(): ShoppingComponent.Builder

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance
    @CheckResult
    fun toolbarActivity(toolbarActivity: ToolbarActivity): Builder

    @BindsInstance
    @CheckResult
    fun parent(parent: ViewGroup): Builder

    @CheckResult
    fun build(): DetailComponent
  }

  @Module
  object DetailProvider {

    @DetailScope
    @JvmStatic
    @Provides
    fun provideStateMap(): MutableMap<String, Int> {
      return LinkedHashMap()
    }
  }

}
