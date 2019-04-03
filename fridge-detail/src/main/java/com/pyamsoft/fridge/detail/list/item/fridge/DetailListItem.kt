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

import android.view.ViewGroup
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.pydroid.arch.BaseUiView
import java.util.Date

internal abstract class DetailListItem protected constructor(
  protected val item: FridgeItem,
  parent: ViewGroup,
  callback: Callback
) : BaseUiView<DetailListItem.Callback>(parent, callback) {

  protected fun commitModel(
    name: String = item.name(),
    expireTime: Date = item.expireTime(),
    presence: Presence = item.presence()
  ) {
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

  interface Callback {

    fun onCommit(item: FridgeItem)

    fun onUpdateModel(item: FridgeItem)

  }

}

