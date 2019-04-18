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
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import dagger.BindsInstance
import dagger.Subcomponent

@DetailScope
@Subcomponent
internal interface DetailComponent {

  @CheckResult
  fun plusCreationComponent(): CreationComponent.Factory

  @CheckResult
  fun plusShoppingComponent(): ShoppingComponent.Factory

  @Subcomponent.Factory
  interface Factory {

    @CheckResult
    fun create(
      @BindsInstance toolbarActivity: ToolbarActivity,
      @BindsInstance parent: ViewGroup
    ): DetailComponent
  }

}
