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

package com.pyamsoft.fridge.detail.add

import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.detail.add.AddNewControllerEvent.AddNew
import com.pyamsoft.fridge.detail.add.AddNewViewEvent.AddNewItemEvent
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.arch.UnitViewState
import javax.inject.Inject

class AddNewItemViewModel @Inject internal constructor(
  entry: FridgeEntry
) : UiViewModel<UnitViewState, AddNewViewEvent, AddNewControllerEvent>(
    initialState = UnitViewState
) {

  private val entryId = entry.id()

  override fun onInit() {
  }

  override fun handleViewEvent(event: AddNewViewEvent) {
    return when (event) {
      is AddNewItemEvent -> publish(AddNew(entryId))
    }
  }

}
