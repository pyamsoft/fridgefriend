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

package com.pyamsoft.fridge.detail.expand.date

import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.arch.UnitControllerEvent
import com.pyamsoft.pydroid.arch.UnitViewEvent
import com.pyamsoft.pydroid.arch.UnitViewState
import com.pyamsoft.pydroid.bus.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class DateSelectViewModel @Inject internal constructor(
    private val dateSelectBus: EventBus<DateSelectPayload>,
) : UiViewModel<UnitViewState, UnitViewEvent, UnitControllerEvent>(
    initialState = UnitViewState
) {

    fun handleDateSelected(
        scope: CoroutineScope,
        itemId: FridgeItem.Id,
        entryId: FridgeEntry.Id,
        year: Int,
        month: Int,
        day: Int,
        onClose: () -> Unit
    ) {
        scope.launch(context = Dispatchers.Default) {
            dateSelectBus.send(DateSelectPayload(itemId, entryId, year, month, day))
            onClose()
        }
    }
}
