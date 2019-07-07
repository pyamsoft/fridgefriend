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

package com.pyamsoft.fridge.base

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModelProvider.Factory
import com.pyamsoft.pydroid.ui.rating.RatingActivity
import javax.inject.Inject

internal abstract class ViewModelFactoryActivity : RatingActivity() {

  @JvmField @Inject internal var factory: Factory? = null

  @CallSuper
  override fun onDestroy() {
    super.onDestroy()
    factory = null
  }

}
