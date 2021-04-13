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

package com.pyamsoft.fridge.detail

import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.arch.UiSavedStateViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import timber.log.Timber

class DetailAddViewModel @AssistedInject internal constructor(
    delegate: DetailListStateModel,
    @Assisted savedState: UiSavedState,
) : BaseAddDetailViewModel<DetailControllerEvent.AddEvent>(delegate, savedState) {

    fun handleAddNew() {
        state.apply {
            val e = entry
            if (e == null) {
                Timber.w("Cannot add new, detail entry null!")
            } else {
                publish(DetailControllerEvent.AddEvent.AddNew(e.id(), listItemPresence))
            }
        }
    }

    @AssistedFactory
    interface Factory : UiSavedStateViewModelProvider<DetailAddViewModel> {
        override fun create(savedState: UiSavedState): DetailAddViewModel
    }
}
