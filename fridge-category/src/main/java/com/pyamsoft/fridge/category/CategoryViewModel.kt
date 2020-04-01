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
 *
 */

package com.pyamsoft.fridge.category

import androidx.lifecycle.viewModelScope
import com.pyamsoft.pydroid.arch.UiViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class CategoryViewModel @Inject internal constructor(
    private val interactor: CategoryInteractor,
    @Named("debug") debug: Boolean
) : UiViewModel<CategoryViewState, CategoryViewEvent, CategoryControllerEvent>(
    initialState = CategoryViewState(categories = emptyList()), debug = debug
) {

    init {
        doOnInit {
            viewModelScope.launch {
                val categories = interactor.loadCategories()
                setState { copy(categories = categories) }
            }
        }
    }

    override fun handleViewEvent(event: CategoryViewEvent) {
    }
}
