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

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.R

internal class EntrySpaceItem internal constructor(
  entry: FridgeEntry
) : EntryItem<EntrySpaceItem, EntrySpaceItem.ViewHolder>(entry) {

  override fun getType(): Int {
    return R.id.id_entry_space_item
  }

  override fun getViewHolder(v: View): ViewHolder {
    return ViewHolder(v)
  }

  override fun getLayoutRes(): Int {
    return R.layout.entry_space_item
  }

  class ViewHolder internal constructor(itemView: View) : RecyclerView.ViewHolder(itemView)

}