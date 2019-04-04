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
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemUiComponent.Callback
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import javax.inject.Inject

internal class DetailListItemUiComponentImpl @Inject internal constructor(
  private val strikethrough: DetailListItemStrikethrough,
  private val name: DetailListItemName,
  private val presence: DetailListItemPresence,
  private val presenter: DetailItemPresenter
) : BaseUiComponent<DetailListItemUiComponent.Callback>(),
  DetailListItemUiComponent,
  DetailItemPresenter.Callback {

  override fun id(): Int {
    return strikethrough.id()
  }

  override fun onBind(owner: LifecycleOwner, savedInstanceState: Bundle?, callback: Callback) {
    owner.doOnDestroy {
      strikethrough.teardown()
      name.teardown()
      presence.teardown()
      presenter.unbind()
    }

    presence.inflate(savedInstanceState)
    name.inflate(savedInstanceState)
    strikethrough.inflate(savedInstanceState)
    presenter.bind(this)
  }

  override fun onLayout(set: ConstraintSet) {
    presence.also {
      set.connect(it.id(), ConstraintSet.TOP, strikethrough.id(), ConstraintSet.TOP)
      set.connect(it.id(), ConstraintSet.BOTTOM, strikethrough.id(), ConstraintSet.BOTTOM)
      set.connect(it.id(), ConstraintSet.END, strikethrough.id(), ConstraintSet.END)
      set.constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
    }

    name.also {
      set.connect(it.id(), ConstraintSet.TOP, strikethrough.id(), ConstraintSet.TOP)
      set.connect(it.id(), ConstraintSet.BOTTOM, strikethrough.id(), ConstraintSet.BOTTOM)
      set.connect(it.id(), ConstraintSet.START, strikethrough.id(), ConstraintSet.START)
      set.connect(it.id(), ConstraintSet.END, presence.id(), ConstraintSet.START)
      set.constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
    }
  }

  override fun onSaveState(outState: Bundle) {
    strikethrough.saveState(outState)
    presence.saveState(outState)
    name.saveState(outState)
  }

  override fun handleNonRealItemDelete(item: FridgeItem) {
    callback.onNonRealItemDelete(item)
  }

  override fun handleNonRealItemCommit(item: FridgeItem) {
    callback.onNonRealItemCommit(item)
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
