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
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

internal class DetailListItemName @Inject internal constructor(
  private val nonPersistedEditableStateMap: MutableMap<String, Int>,
  @Named("item_editable") private val editable: Boolean,
  item: FridgeItem,
  parent: ViewGroup,
  callback: DetailListItemName.Callback
) : DetailListItem<DetailListItemName.Callback>(item, parent, callback) {

  override val layout: Int = R.layout.detail_list_item_name

  override val layoutRoot by lazyView<ViewGroup>(R.id.detail_item_name)
  private val nameView by lazyView<EditText>(R.id.detail_item_name_editable)

  private var nameWatcher: TextWatcher? = null

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    nameView.setText(item.name())

    // Restore cursor position from the list widge storage map
    if (nonPersistedEditableStateMap.containsKey(item.id())) {
      val location = nonPersistedEditableStateMap[item.id()] ?: 0
      val restoreTo = Math.min(item.name().length, location)
      Timber.d("Restore edit text selection from storage map for: ${item.id()}: $restoreTo")
      Timber.d("CommitName: ${item.name()} [${item.name().length}]")
      nameView.setSelection(restoreTo)
      nonPersistedEditableStateMap.remove(item.id())
    }

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
    nameView.text.clear()
  }

  override fun onMadeReal() {

  }

  private fun commit() {
    saveEditingState()
    val name = nameView.text.toString()
    callback.commitName(item, name)
  }

  private fun saveEditingState() {
    // Commit editing location to the storage map
    val location = nameView.selectionEnd
    Timber.d("Save edit text selection from storage map for: ${item.id()}: $location")
    nonPersistedEditableStateMap[item.id()] = location
  }

  fun focus() {
    nameView.requestFocus()
  }

  interface Callback : DetailListItem.Callback {

    fun commitName(oldItem: FridgeItem, name: String)

  }

}

