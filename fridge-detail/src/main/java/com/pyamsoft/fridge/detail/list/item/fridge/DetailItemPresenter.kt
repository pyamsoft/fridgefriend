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

import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailConstants
import com.pyamsoft.fridge.detail.list.DetailListInteractor
import com.pyamsoft.fridge.detail.list.item.fridge.DetailItemPresenter.Callback
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

// Not scoped by Dagger, see DetailItemUiComponentFactory for scoping implementation
internal class DetailItemPresenter @Inject internal constructor(
  private val interactor: DetailListInteractor
) : BasePresenter<Unit, Callback>(RxBus.empty()),
  DetailListItemView.Callback {

  private var updateDisposable by singleDisposable()
  private var deleteDisposable by singleDisposable()

  override fun onBind() {
  }

  override fun onUnbind() {
    // Don't clear Disposables here as it may need to outlive the View
    // since the final commit happens as the View is tearing down
  }

  override fun onCommit(item: FridgeItem) {
    // A delete operation will stop an update operation
    updateDisposable = Completable.complete()
      .delay(DetailConstants.COMMIT_TIMEOUT_DURATION, DetailConstants.COMMIT_TIMEOUT_UNIT)
      .andThen(interactor.commit(item))
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .doAfterTerminate { updateDisposable.tryDispose() }
      .subscribe({ }, {
        Timber.e(it, "Error updating item: ${item.id()}")
        callback.handleUpdateItemError(it)
      })
  }

  override fun onDelete(item: FridgeItem) {
    // Stop any pending updates
    updateDisposable.tryDispose()

    // If this item is not real, its an empty placeholder
    // The user may still wish to delete it from their list
    // in case they have too many placeholders.
    // Directly call the realtime delete callback as if the
    // delete had actually happened
    if (!item.isReal()) {
      Timber.w("Delete called on a non-real item: $item, fake callback")
      callback.handleNonRealItemDelete(item)
      return
    }

    deleteDisposable = interactor.delete(item)
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .doAfterTerminate { deleteDisposable.tryDispose() }
      .subscribe({ }, {
        Timber.e(it, "Error deleting item: ${item.id()}")
        callback.handleDeleteItemError(it)
      })
  }

  override fun onUpdateModel(item: FridgeItem) {
    callback.handleModelUpdate(item)
  }

  interface Callback {

    fun handleModelUpdate(item: FridgeItem)

    fun handleNonRealItemDelete(item: FridgeItem)

    fun handleUpdateItemError(throwable: Throwable)

    fun handleDeleteItemError(throwable: Throwable)

  }

}
