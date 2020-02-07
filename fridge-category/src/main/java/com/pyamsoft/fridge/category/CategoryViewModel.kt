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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class CategoryViewModel @Inject internal constructor(
    private val interactor: CategoryInteractor
) : UiViewModel<CategoryViewState, CategoryViewEvent, CategoryControllerEvent>(
    initialState = CategoryViewState(
        largeCategories = emptyList(),
        smallCategories = emptyList()
    )
) {

    override fun handleViewEvent(event: CategoryViewEvent) {
        return when (event) {
            is CategoryViewEvent.ViewReadyForData -> fetchData()
        }
    }

    private fun fetchData() {
        viewModelScope.launch(context = Dispatchers.Default) {
            val largeList = interactor.loadLargeCategories()
            setState { copy(largeCategories = largeList) }
        }

        viewModelScope.launch(context = Dispatchers.Default) {
            val smallList = interactor.loadSmallCategories()
            setState { copy(smallCategories = smallList) }
        }
    }
}
