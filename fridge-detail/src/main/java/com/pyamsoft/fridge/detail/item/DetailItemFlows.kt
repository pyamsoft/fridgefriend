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

package com.pyamsoft.fridge.detail.item

import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailViewState
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

data class DetailListItemViewState internal constructor(
    val item: FridgeItem,
    val expirationRange: DetailViewState.ExpirationRange?,
    val isSameDayExpired: DetailViewState.IsSameDayExpired?
) : UiViewState

sealed class DetailItemViewEvent : UiViewEvent {

    object ExpandItem : DetailItemViewEvent()

    object CommitPresence : DetailItemViewEvent()
}
