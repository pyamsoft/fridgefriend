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

package com.pyamsoft.fridge.detail.base

import com.pyamsoft.fridge.core.currentDate
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailInteractor
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.core.ResultWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

internal class UpdateDelegate
internal constructor(
    interactor: DetailInteractor,
    handleError: CoroutineScope.(Throwable) -> Unit,
) {

  private var interactor: DetailInteractor? = interactor
  private var handleError: (CoroutineScope.(Throwable) -> Unit)? = handleError

  fun teardown() {
    interactor = null
    handleError = null
  }

  private val updateRunner =
      highlander<
          ResultWrapper<Unit>, FridgeItem, suspend (item: FridgeItem) -> ResultWrapper<Unit>> {
          item,
          doUpdate ->
        doUpdate(item.makeReal())
      }

  private fun CoroutineScope.update(
      item: FridgeItem,
      doUpdate: suspend (item: FridgeItem) -> ResultWrapper<Unit>
  ) {
    launch(context = Dispatchers.Default) {
      updateRunner
          .call(item, doUpdate)
          .onFailure { Timber.e(it, "Error updating item $item") }
          .onFailure { err ->
            val handler = requireNotNull(handleError)
            handler(err)
          }
    }
  }

  internal fun consumeItem(scope: CoroutineScope, item: FridgeItem) {
    scope.update(item) { requireNotNull(interactor).commit(it.consume(currentDate())) }
  }

  internal fun restoreItem(scope: CoroutineScope, item: FridgeItem) {
    scope.update(item) {
      requireNotNull(interactor).commit(it.invalidateConsumption().invalidateSpoiled())
    }
  }

  internal fun spoilItem(scope: CoroutineScope, item: FridgeItem) {
    scope.update(item) { requireNotNull(interactor).commit(it.spoil(currentDate())) }
  }

  internal fun deleteItem(scope: CoroutineScope, item: FridgeItem) {
    scope.update(item) { requireNotNull(interactor).delete(it, true) }
  }

  internal fun updateItem(scope: CoroutineScope, item: FridgeItem) {
    val updated =
        item.run {
          val dateOfPurchase = this.purchaseTime()
          if (this.presence() == FridgeItem.Presence.HAVE) {
            // If we are HAVE but don't have a purchase date, we just purchased the item!
            if (dateOfPurchase == null) {
              val now = currentDate()
              Timber.d("${item.name()} purchased! $now")
              return@run this.purchaseTime(now)
            }
          } else {
            // If we are NEED but have a purchase date, we must invalidate the date
            if (dateOfPurchase != null) {
              Timber.d("${item.name()} purchase date cleared")
              return@run this.invalidatePurchase()
            }
          }

          // No side effects
          return@run this
        }

    scope.update(updated) { requireNotNull(interactor).commit(it) }
  }
}
