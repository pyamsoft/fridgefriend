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

package com.pyamsoft.fridge.entry.list

import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

data class EntryListViewState(
  val isLoading: Loading?,
  val throwable: Throwable?,
  val entries: List<FridgeEntry>
) : UiViewState {

  data class Loading(val isLoading: Boolean)

}

sealed class EntryListViewEvent : UiViewEvent {

  object ForceRefresh : EntryListViewEvent()

  data class OpenEntry internal constructor(val entry: FridgeEntry) : EntryListViewEvent()

}

sealed class EntryListControllerEvent : UiControllerEvent {

  data class OpenForEditing internal constructor(
    val entry: FridgeEntry
  ) : EntryListControllerEvent()

}
