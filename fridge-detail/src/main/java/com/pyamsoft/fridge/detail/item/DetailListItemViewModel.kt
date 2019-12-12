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

package com.pyamsoft.fridge.detail.item

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.core.Preferences
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.detail.DetailInteractor
import com.pyamsoft.fridge.detail.base.BaseUpdaterViewModel
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.ExpandDetails
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CommitPresence
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.ExpandItem
import com.pyamsoft.pydroid.arch.EventBus
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

class DetailListItemViewModel @Inject internal constructor(
    private val interactor: DetailInteractor,
    private val fakeRealtime: EventBus<FridgeItemChangeEvent>
) : BaseUpdaterViewModel<DetailItemViewState, DetailItemViewEvent, DetailItemControllerEvent>(
    initialState = DetailItemViewState(
        item = null,
        expirationRange = interactor.getExpiringSoonRange(),
        isSameDayExpired = interactor.isSameDayExpired(),
        throwable = null
    )
) {

    init {
        var expiringSoonUnregister: Preferences.Unregister? = null

        doOnInit {
            expiringSoonUnregister = interactor.watchForExpiringSoonChanges { newRange ->
                setState { copy(expirationRange = newRange) }
            }
        }

        doOnTeardown {
            expiringSoonUnregister?.unregister()
            expiringSoonUnregister = null
        }

        var isSameDayExpiredUnregister: Preferences.Unregister? = null

        doOnInit {
            isSameDayExpiredUnregister = interactor.watchForSameDayExpiredChange { newSameDay ->
                setState { copy(isSameDayExpired = newSameDay) }
            }
        }

        doOnTeardown {
            isSameDayExpiredUnregister?.unregister()
            isSameDayExpiredUnregister = null
        }
    }

    override fun handleViewEvent(event: DetailItemViewEvent) {
        return when (event) {
            is CommitPresence -> commitPresence(event.oldItem, event.presence)
            is ExpandItem -> withState { item?.let { expandItem(it) } }
        }
    }

    private fun commitPresence(
        oldItem: FridgeItem,
        presence: Presence
    ) {
        if (!oldItem.isReal()) {
            Timber.w("Cannot commit change for not-real item: $oldItem")
            return
        }

        val item = oldItem.presence(presence)
        val updated = item.run {
            val dateOfPurchase = purchaseTime()
            if (presence() == HAVE) {
                if (dateOfPurchase == null) {
                    val now = Date()
                    Timber.d("${item.name()} purchased! $now")
                    return@run purchaseTime(now)
                }
            } else {
                if (dateOfPurchase != null) {
                    Timber.d("${item.name()} purchase date cleared")
                    return@run invalidatePurchase()
                }
            }

            return@run this
        }

        viewModelScope.launch {
            update(updated, doUpdate = { interactor.commit(it) }, onError = { handleError(it) })
        }
    }

    private fun handleFakeDelete(item: FridgeItem) {
        viewModelScope.launch {
            Timber.w("Remove called on a non-real item: $item, fake callback")
            fakeRealtime.send(FridgeItemChangeEvent.Delete(item))
        }
    }

    private fun handleError(throwable: Throwable) {
        setState { copy(throwable = throwable) }
    }

    fun bind(item: FridgeItem) {
        setState { copy(item = item) }
    }

    fun unbind() {
        setState { copy(item = null) }
    }

    fun consume() {
        withState {
            item?.let { fi ->
                if (fi.isReal()) {
                    update(fi, doUpdate = { interactor.consume(it) }, onError = { handleError(it) })
                }
            }
        }
    }

    fun restore() {
        withState {
            item?.let { fi ->
                if (fi.isReal()) {
                    update(fi, doUpdate = { interactor.restore(it) }, onError = { handleError(it) })
                }
            }
        }
    }

    fun spoil() {
        withState {
            item?.let { fi ->
                if (fi.isReal()) {
                    update(fi, doUpdate = { interactor.spoil(it) }, onError = { handleError(it) })
                }
            }
        }
    }

    fun delete() {
        withState {
            item?.let { fi ->
                if (fi.isReal()) {
                    update(fi, doUpdate = { interactor.delete(it) }, onError = { handleError(it) })
                } else {
                    handleFakeDelete(fi)
                }
            }
        }
    }

    private fun expandItem(item: FridgeItem) {
        publish(ExpandDetails(item))
    }
}
