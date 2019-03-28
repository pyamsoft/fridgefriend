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

package com.pyamsoft.fridge.detail.list.item.fridge

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.core.view.isVisible
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.pydroid.arch.UiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.arch.InvalidIdException
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.tintWith
import timber.log.Timber
import java.util.Date
import kotlin.LazyThreadSafetyMode.NONE

internal class DetailListItemView internal constructor(
  private val item: FridgeItem,
  private val parent: View,
  private val entryId: String,
  private val theming: Theming,
  private val imageLoader: ImageLoader,
  private val nonPersistedEditableStateMap: MutableMap<String, Int>,
  private val callback: DetailListItemView.Callback
) : UiView {

  private val itemName by lazy(NONE) { parent.findViewById<EditText>(R.id.detail_item_name) }
  private val itemDelete by lazy(NONE) { parent.findViewById<ImageView>(R.id.detail_item_delete) }

  private var nameWatcher: TextWatcher? = null
  private var deleteIconLoaded: Loaded? = null

  override fun id(): Int {
    throw InvalidIdException
  }

  override fun inflate(savedInstanceState: Bundle?) {
    removeListeners()

    setupName()
    setupDelete()
  }

  override fun saveState(outState: Bundle) {
  }

  override fun teardown() {
    removeListeners()

    // Cleaup
    itemName.text.clear()

    itemDelete.setOnClickListener(null)
    itemDelete.setImageDrawable(null)
    deleteIconLoaded?.dispose()
    deleteIconLoaded = null
  }

  private fun setupDelete() {
    deleteIconLoaded?.dispose()
    deleteIconLoaded = null

    itemDelete.isVisible = true
    deleteIconLoaded = imageLoader.load(R.drawable.ic_close_24dp)
      .mutate { drawable ->
        val color: Int
        if (theming.isDarkTheme()) {
          color = R.color.white
        } else {
          color = R.color.black
        }
        return@mutate drawable.tintWith(parent.context, color)
      }.into(itemDelete)

    itemDelete.setOnDebouncedClickListener {
      itemDelete.setOnClickListener(null)
      callback.onDelete(item)
    }
  }

  private fun setupName() {
    itemName.setText(item.name())

    // Restore cursor position from the list widge storage map
    if (nonPersistedEditableStateMap.containsKey(item.id())) {
      val location = nonPersistedEditableStateMap[item.id()] ?: 0
      val restoreTo = Math.min(item.name().length, location)
      Timber.d("Restore edit text selection from storage map for: ${item.id()}: $restoreTo")
      Timber.d("Name: ${item.name()} [${item.name().length}]")
      itemName.setSelection(restoreTo)
      nonPersistedEditableStateMap.remove(item.id())
    }

    val watcher = object : TextWatcher {

      override fun afterTextChanged(s: Editable?) {
        if (s != null) {
          commit(name = s.toString())
        }
      }

      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
      }

      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
      }

    }
    itemName.addTextChangedListener(watcher)
    nameWatcher = watcher
  }

  private fun removeListeners() {
    // Unbind all listeners
    nameWatcher?.let { itemName.removeTextChangedListener(it) }
    nameWatcher = null
  }

  private fun commit(
    name: String = itemName.text.toString(),
    expireTime: Date = Date(),
    presence: Presence = Presence.NEED
  ) {
    saveEditingState()
    commitModel(name, expireTime, presence)
  }

  private fun saveEditingState() {
    // Commit editing location to the storage map
    val location = itemName.selectionEnd
    Timber.d("Save edit text selection from storage map for: ${item.id()}: $location")
    nonPersistedEditableStateMap[item.id()] = location
  }

  private fun commitModel(
    name: String = item.name(),
    expireTime: Date = item.expireTime(),
    presence: Presence = item.presence()
  ) {
    if (item.entryId() == entryId) {

      // Commit a new model from a dif
      val oldModel = item
      var newModel = item
      if (oldModel.name() != name) {
        newModel = newModel.name(name)
      }
      if (oldModel.expireTime() != expireTime) {
        newModel = newModel.expireTime(expireTime)
      }
      if (oldModel.presence() != presence) {
        newModel = newModel.presence(presence)
      }

      if (newModel != oldModel) {
        callback.onUpdateModel(newModel)
      }

      callback.onCommit(newModel)
    }
  }

  interface Callback {

    fun onCommit(item: FridgeItem)

    fun onDelete(item: FridgeItem)

    fun onUpdateModel(item: FridgeItem)

  }
}

