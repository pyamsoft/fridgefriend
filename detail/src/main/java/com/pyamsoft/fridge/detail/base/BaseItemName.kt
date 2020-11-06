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

package com.pyamsoft.fridge.detail.base

import android.view.ViewGroup
import com.pyamsoft.fridge.detail.databinding.DetailListItemNameBinding
import com.pyamsoft.fridge.ui.view.UiEditText
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

abstract class BaseItemName<S : UiViewState, V : UiViewEvent> protected constructor(
    parent: ViewGroup
) : UiEditText<S, V, DetailListItemNameBinding>(parent) {

    final override val viewBinding = DetailListItemNameBinding::inflate

    final override val layoutRoot by boundView { detailItemName }

}
