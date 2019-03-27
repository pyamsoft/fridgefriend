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

package com.pyamsoft.fridge.detail.list

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.tintWith
import timber.log.Timber
import java.util.Date

internal class DetailListItem internal constructor(
  item: FridgeItem,
  private val theming: Theming,
  private val imageLoader: ImageLoader,
  private val nonPersistedEditableStateMap: MutableMap<String, Int>,
  private val entryId: String,
  private val callback: DetailListItem.Callback
) : DetailItem<DetailListItem, DetailListItem.ViewHolder>(item) {

  override fun getType(): Int {
    return R.id.id_item_list_item
  }

  override fun getViewHolder(v: View): ViewHolder {
    return ViewHolder(v, theming, imageLoader, nonPersistedEditableStateMap)
  }

  override fun getLayoutRes(): Int {
    return R.layout.detail_list_item
  }

  override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
    super.bindView(holder, payloads)
    holder.bind(this)
  }

  override fun unbindView(holder: ViewHolder) {
    super.unbindView(holder)
    holder.unbind()
  }

  override fun finalCommitOnDestroy() {
    Timber.d("Performing final commit on dying list item: $model")
    commit(finalUpdate = true)
  }

  private fun commit(
    name: String = model.name(),
    expireTime: Date = model.expireTime(),
    presence: Presence = model.presence(),
    finalUpdate: Boolean
  ) {
    if (model.entryId() == entryId) {

      // Commit a new model from a dif
      val oldModel = model
      var newModel = model
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
        withModel(newModel)
      }

      if (finalUpdate) {
        Timber.d("Final item commit on unbind: $model")
      }

      callback.onCommit(model, finalUpdate)
    }
  }

  class ViewHolder internal constructor(
    itemView: View,
    private val theming: Theming,
    private val imageLoader: ImageLoader,
    private val nonPersistedEditableStateMap: MutableMap<String, Int>
  ) : RecyclerView.ViewHolder(itemView) {

    private var boundItem: DetailListItem? = null

    private val itemName = itemView.findViewById<EditText>(R.id.detail_item_name)
    private val itemDelete = itemView.findViewById<ImageView>(R.id.detail_item_delete)

    private var nameWatcher: TextWatcher? = null
    private var deleteIconLoaded: Loaded? = null

    fun bind(item: DetailListItem) {
      boundItem = item
      removeListeners()

      setupName(item)
      setupDelete(item)
    }

    private fun setupDelete(item: DetailListItem) {
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
          return@mutate drawable.tintWith(itemView.context, color)
        }.into(itemDelete)

      itemDelete.setOnDebouncedClickListener {
        itemDelete.setOnClickListener(null)

        item.callback.onDelete(item.model)
      }
    }

    private fun setupName(item: DetailListItem) {
      itemName.setText(item.model.name())

      // Restore cursor position from the list widge storage map
      if (nonPersistedEditableStateMap.containsKey(item.model.id())) {
        val location = nonPersistedEditableStateMap[item.model.id()] ?: 0
        Timber.d("Restore edit text selection from storage map for: ${item.model.id()}: $location")
        itemName.setSelection(location)
        nonPersistedEditableStateMap.remove(item.model.id())
      }

      val watcher = object : TextWatcher {

        override fun afterTextChanged(s: Editable?) {
          if (s != null) {
            commit(name = s.toString(), finalUpdate = false)
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

    fun unbind() {
      removeListeners()

      // Cleaup
      itemName.text.clear()

      itemDelete.setOnClickListener(null)
      itemDelete.setImageDrawable(null)
      deleteIconLoaded?.dispose()
      deleteIconLoaded = null

      boundItem = null
    }

    private fun commit(
      name: String = itemName.text.toString(),
      expireTime: Date = Date(),
      presence: Presence = Presence.NEED,
      finalUpdate: Boolean
    ) {
      boundItem?.let { item ->
        saveEditingState()
        item.commit(name, expireTime, presence, finalUpdate)
      }
    }

    private fun saveEditingState() {
      // Commit editing location to the storage map
      val item = boundItem
      if (item != null) {
        val location = itemName.selectionEnd
        Timber.d("Save edit text selection from storage map for: ${item.model.id()}: $location")
        nonPersistedEditableStateMap[item.model.id()] = location
      }
    }

  }

  interface Callback {

    fun onCommit(item: FridgeItem, finalUpdate: Boolean)

    fun onDelete(item: FridgeItem)

  }

}