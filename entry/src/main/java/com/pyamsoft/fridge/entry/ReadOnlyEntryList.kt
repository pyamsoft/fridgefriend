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

package com.pyamsoft.fridge.entry

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.entry.item.EntryItemComponent
import javax.inject.Inject

class ReadOnlyEntryList
@Inject
internal constructor(
    owner: LifecycleOwner,
    parent: ViewGroup,
    factory: EntryItemComponent.Factory,
) : BaseEntryList<ReadOnlyListEvents>(owner, parent, factory) {

  override fun onRefresh() {
    publish(ReadOnlyListEvents.ForceRefresh)
  }

  override fun onClick(index: Int) {
    publish(ReadOnlyListEvents.Select(index))
  }

  override fun onLongPress(index: Int) {
    publish(ReadOnlyListEvents.Select(index))
  }
}
