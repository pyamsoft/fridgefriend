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
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.detail.DetailInteractor
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.ExpandDetails
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CloseItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CommitCount
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CommitName
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CommitPresence
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.ConsumeItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.DeleteItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.ExpandItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.PickDate
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.SpoilItem
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.EventBus
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

class DetailListItemViewModel @Inject internal constructor(
    fakeRealtime: EventBus<FridgeItemChangeEvent>,
    private val interactor: DetailInteractor,
    private val item: FridgeItem
) : DetailItemViewModel(item, fakeRealtime) {

    private val updateRunner = highlander<Unit, FridgeItem> { item ->
        try {
            interactor.commit(item.makeReal())
        } catch (error: Throwable) {
            error.onActualError { e ->
                Timber.e(e, "Error updating item: ${item.id()}")
            }
        }
    }

    override fun onInit() {
    }

    override fun handleViewEvent(event: DetailItemViewEvent) {
        return when (event) {
            is CommitPresence -> commitPresence(event.oldItem, event.presence)
            is ExpandItem -> expandItem(event.item)
            is CommitName, is PickDate, is CloseItem, is DeleteItem, is ConsumeItem, is SpoilItem, is CommitCount -> {
                Timber.d("Ignore event: $event")
            }
        }
    }

    private fun commitPresence(
        oldItem: FridgeItem,
        presence: Presence
    ) {
        commitItem(item = oldItem.presence(presence))
    }

    private fun commitItem(item: FridgeItem) {
        viewModelScope.launch {
            if (item.isReal()) {
                updateRunner.call(item.run {
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
                })
            } else {
                Timber.w("Cannot commit change for not-real item: $item")
            }
        }
    }

    fun consume() {
        remove(item, doRemove = { interactor.consume(it) })
    }

    fun spoil() {
        remove(item, doRemove = { interactor.spoil(it) })
    }

    private fun expandItem(item: FridgeItem) {
        publish(ExpandDetails(item))
    }
}
