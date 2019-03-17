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

package com.pyamsoft.fridge.create

import android.view.ViewGroup
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.create.CreateComponent.CreateProvider
import com.pyamsoft.pydroid.ui.widget.shadow.DropshadowView
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent

@CreateScope
@Subcomponent(modules = [CreateProvider::class, CreateModule::class])
internal interface CreateComponent {

  fun inject(dialog: EntryCreateDialog)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance
    @CheckResult
    fun parent(parent: ViewGroup): Builder

    @CheckResult
    fun build(): CreateComponent
  }

  @Module
  object CreateProvider {

    @Provides
    @JvmStatic
    fun provideDropshadow(parent: ViewGroup): DropshadowView {
      return DropshadowView(parent)
    }

  }

}
