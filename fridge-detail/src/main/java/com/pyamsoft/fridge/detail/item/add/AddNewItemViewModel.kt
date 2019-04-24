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

package com.pyamsoft.fridge.detail.item.add

import com.pyamsoft.fridge.detail.item.add.AddNewItemHandler.AddNewEvent
import com.pyamsoft.fridge.detail.item.add.AddNewItemViewModel.AddNewState
import com.pyamsoft.pydroid.arch.UiEventHandler
import com.pyamsoft.pydroid.arch.UiState
import com.pyamsoft.pydroid.arch.UiViewModel
import javax.inject.Inject

internal class AddNewItemViewModel @Inject internal constructor(
  private val handler: UiEventHandler<AddNewEvent, AddNewItemView.Callback>
) : UiViewModel<AddNewState>(
  initialState = AddNewState(isAdding = false)
), AddNewItemView.Callback {

  override fun onBind() {
    handler.handle(this).disposeOnDestroy()
  }

  override fun onUnbind() {
  }

  override fun onAddNewClicked() {
    setUniqueState(true, old = { it.isAdding }) { state, value ->
      state.copy(isAdding = value)
    }
  }

  data class AddNewState(val isAdding: Boolean) : UiState

}
