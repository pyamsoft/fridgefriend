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
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.list.item.fridge.DetailListItemUiComponent.Callback
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import com.pyamsoft.pydroid.ui.arch.InvalidIdException
import javax.inject.Inject

internal class DetailListItemUiComponentImpl @Inject internal constructor(
  private val name: DetailListItemName,
  private val presenter: DetailItemPresenter
) : BaseUiComponent<DetailListItemUiComponent.Callback>(),
  DetailListItemUiComponent,
  DetailItemPresenter.Callback {

  override fun id(): Int {
    throw InvalidIdException
  }

  override fun onBind(owner: LifecycleOwner, savedInstanceState: Bundle?, callback: Callback) {
    owner.doOnDestroy {
      name.teardown()
      presenter.unbind()
    }

    name.inflate(savedInstanceState)
    presenter.bind(this)
  }

  override fun onSaveState(outState: Bundle) {
    name.saveState(outState)
  }

  override fun handleNonRealItemDelete(item: FridgeItem) {
    callback.onNonRealItemDelete(item)
  }

  override fun handleUpdateItemError(throwable: Throwable) {
    callback.onUpdateItemError(throwable)
  }

  override fun handleDeleteItemError(throwable: Throwable) {
    callback.onDeleteItemError(throwable)
  }

  override fun handleModelUpdate(item: FridgeItem) {
    callback.onModelUpdate(item)
  }

  override fun deleteSelf(item: FridgeItem) {
    presenter.deleteSelf(item)
  }

}
