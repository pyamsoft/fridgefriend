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

package com.pyamsoft.fridge.detail.item.fridge

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemName.Callback
import javax.inject.Inject
import javax.inject.Named

internal class DetailListItemName @Inject internal constructor(
  @Named("item_editable") private val editable: Boolean,
  item: FridgeItem,
  parent: ViewGroup,
  callback: Callback
) : DetailListItem<Callback>(item, parent, callback) {

  override val layout: Int = R.layout.detail_list_item_name

  override val layoutRoot by boundView<ViewGroup>(R.id.detail_item_name)
  private val nameView by boundView<EditText>(R.id.detail_item_name_editable)

  private var nameWatcher: TextWatcher? = null

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    nameView.setText(item.name())
    if (editable && !item.isArchived()) {
      val watcher = object : TextWatcher {

        override fun afterTextChanged(s: Editable?) {
          commit()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

      }
      nameView.addTextChangedListener(watcher)
      nameWatcher = watcher
    } else {
      nameView.setNotEditable()
    }
  }

  override fun onTeardown() {
    // Unbind all listeners
    nameWatcher?.let { nameView.removeTextChangedListener(it) }
    nameWatcher = null

    // Cleaup
    nameView.clearFocus()
    nameView.text.clear()
  }

  override fun onItemUpdated() {

  }

  private fun commit() {
    val name = nameView.text.toString()
    callback.commitName(item, name)
  }

  fun focus() {
    nameView.requestFocus()
  }

  interface Callback : DetailListItem.Callback {

    fun commitName(oldItem: FridgeItem, name: String)

  }

}

