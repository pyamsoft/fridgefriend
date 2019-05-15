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

package com.pyamsoft.fridge.entry.action

import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.action.EntryActionControllerEvent.OpenCreate
import com.pyamsoft.fridge.entry.action.EntryActionControllerEvent.OpenShopping
import com.pyamsoft.fridge.entry.action.EntryActionViewEvent.CreateClicked
import com.pyamsoft.fridge.entry.action.EntryActionViewEvent.FirstAnimationDone
import com.pyamsoft.fridge.entry.action.EntryActionViewEvent.ShopClicked
import com.pyamsoft.fridge.entry.action.EntryActionViewEvent.SpacingCalculated
import com.pyamsoft.fridge.entry.action.EntryActionViewState.Spacing
import com.pyamsoft.pydroid.arch.impl.BaseUiViewModel
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class EntryActionViewModel @Inject internal constructor(
  private val interactor: EntryActionInteractor
) : BaseUiViewModel<EntryActionViewState, EntryActionViewEvent, EntryActionControllerEvent>(
    initialState = EntryActionViewState(
        throwable = null,
        spacing = Spacing(isLaidOut = false, isFirstAnimationDone = false, gap = 0, margin = 0)
    )
) {

  private var createDisposable by singleDisposable()

  override fun handleViewEvent(event: EntryActionViewEvent) {
    return when (event) {
      is CreateClicked -> handleCreateClicked()
      is ShopClicked -> publish(OpenShopping)
      is SpacingCalculated -> showWithSpacing(event.gap, event.margin)
      FirstAnimationDone -> showAfterFirstAnimationDone()
    }
  }

  private fun handleCreateClicked() {
    createDisposable = interactor.create()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { createDisposable.tryDispose() }
        .subscribe({
          handleCreate(it)
        }, {
          Timber.e(it, "Error creating new entry")
          handleCreateError(it)
        })
  }

  private fun handleCreate(entry: FridgeEntry) {
    publish(OpenCreate(entry))
  }

  private fun handleCreateError(throwable: Throwable) {
    setState { copy(throwable = throwable) }
  }

  private fun showWithSpacing(
    gap: Int,
    margin: Int
  ) {
    setState {
      copy(
          spacing = spacing.copy(
              isLaidOut = true,
              gap = gap,
              margin = margin
          )
      )
    }
  }

  private fun showAfterFirstAnimationDone() {
    setState {
      copy(spacing = spacing.copy(isFirstAnimationDone = true))
    }
  }

}
