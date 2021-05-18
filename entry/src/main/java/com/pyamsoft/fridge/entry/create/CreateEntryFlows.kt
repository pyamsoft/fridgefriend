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

package com.pyamsoft.fridge.entry.create

import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

data class CreateEntryViewState
internal constructor(val entry: FridgeEntry?, val working: Boolean, val throwable: Throwable?) :
    UiViewState

sealed class CreateEntryViewEvent : UiViewEvent {

  data class NameChanged internal constructor(val name: String) : CreateEntryViewEvent()

  object Commit : CreateEntryViewEvent()
}

sealed class CreateEntryControllerEvent : UiControllerEvent {

  object Commit : CreateEntryControllerEvent()
}
