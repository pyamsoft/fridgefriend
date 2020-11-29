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

package com.pyamsoft.fridge.entry

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.ui.BottomOffset
import com.pyamsoft.pydroid.arch.EventConsumer
import com.pyamsoft.pydroid.arch.UiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class EntryAddViewModel @Inject internal constructor(
    bottomOffsetBus: EventConsumer<BottomOffset>,
) : UiViewModel<EntryAddViewState, EntryAddViewEvent, EntryAddControllerEvent>(
    EntryAddViewState(bottomOffset = 0)
) {

    init {
        viewModelScope.launch(context = Dispatchers.Default) {
            bottomOffsetBus.onEvent { setState { copy(bottomOffset = it.height) } }
        }
    }

    override fun handleViewEvent(event: EntryAddViewEvent) {
        return when (event) {
            is EntryAddViewEvent.AddNew -> handleAddNew()
        }
    }

    private fun handleAddNew() {
        Timber.d("Add new entry")
        publish(EntryAddControllerEvent.AddEntry)
    }

}
