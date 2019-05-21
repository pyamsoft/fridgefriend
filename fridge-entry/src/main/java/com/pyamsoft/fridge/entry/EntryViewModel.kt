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

package com.pyamsoft.fridge.entry

import com.pyamsoft.fridge.db.PersistentEntries
import com.pyamsoft.fridge.entry.EntryControllerEvent.NavigateToSettings
import com.pyamsoft.fridge.entry.EntryControllerEvent.PushHave
import com.pyamsoft.fridge.entry.EntryControllerEvent.PushNeed
import com.pyamsoft.fridge.entry.EntryViewEvent.OpenHave
import com.pyamsoft.fridge.entry.EntryViewEvent.OpenNeed
import com.pyamsoft.fridge.entry.EntryViewEvent.SettingsNavigate
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class EntryViewModel @Inject internal constructor(
  private val persistentEntries: PersistentEntries
) : UiViewModel<EntryViewState, EntryViewEvent, EntryControllerEvent>(
    initialState = EntryViewState(haveEntry = null, needEntry = null, isSettingsItemVisible = true)
) {

  private var haveDisposable by singleDisposable()
  private var needDisposable by singleDisposable()

  override fun handleViewEvent(event: EntryViewEvent) {
    return when (event) {
      is OpenHave -> publish(PushHave(event.entry))
      is OpenNeed -> publish(PushNeed(event.entry))
      is SettingsNavigate -> publish(NavigateToSettings)
    }
  }

  fun showMenu(visible: Boolean) {
    setState { copy(isSettingsItemVisible = visible) }
  }

  override fun onCleared() {
    haveDisposable.tryDispose()
    needDisposable.tryDispose()
  }

  fun fetchEntries() {
    haveDisposable = persistentEntries.getHaveEntry()
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .doAfterTerminate { haveDisposable.tryDispose() }
        .subscribe(Consumer {
          setState {
            publish(PushHave(it))
            copy(haveEntry = it)
          }
        })

    needDisposable = persistentEntries.getNeedEntry()
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .doAfterTerminate { needDisposable.tryDispose() }
        .subscribe(Consumer { setState { copy(needEntry = it) } })
  }

}
