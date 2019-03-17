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

package com.pyamsoft.fridge.create.title

import android.view.ViewGroup
import com.google.android.material.textfield.TextInputLayout
import com.pyamsoft.fridge.create.R
import com.pyamsoft.fridge.create.title.CreateTitle.Callback
import com.pyamsoft.pydroid.arch.BaseUiView
import javax.inject.Inject

internal class CreateTitle @Inject internal constructor(
  parent: ViewGroup,
  callback: Callback
) : BaseUiView<Callback>(parent, callback) {

  override val layout: Int = R.layout.create_title

  override val layoutRoot by lazyView<TextInputLayout>(R.id.entry_create_title)

  interface Callback
}
