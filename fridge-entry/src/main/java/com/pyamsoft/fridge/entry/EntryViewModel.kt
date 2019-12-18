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

import androidx.annotation.CheckResult
import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.PersistentEntries
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.EntryControllerEvent.NavigateToSettings
import com.pyamsoft.fridge.entry.EntryControllerEvent.PushHave
import com.pyamsoft.fridge.entry.EntryControllerEvent.PushNearby
import com.pyamsoft.fridge.entry.EntryControllerEvent.PushNeed
import com.pyamsoft.fridge.entry.EntryViewEvent.OpenHave
import com.pyamsoft.fridge.entry.EntryViewEvent.OpenNearby
import com.pyamsoft.fridge.entry.EntryViewEvent.OpenNeed
import com.pyamsoft.fridge.entry.EntryViewEvent.SettingsNavigate
import com.pyamsoft.fridge.locator.MapPermission
import com.pyamsoft.pydroid.arch.UiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class EntryViewModel @Inject internal constructor(
    private val mapPermission: MapPermission,
    persistentEntries: PersistentEntries,
    @Named("app_name") appNameRes: Int
) : UiViewModel<EntryViewState, EntryViewEvent, EntryControllerEvent>(
    initialState = EntryViewState(
        entry = null,
        isSettingsItemVisible = true,
        appNameRes = appNameRes
    )
) {

    init {
        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                val entry = persistentEntries.getPersistentEntry()
                setState { copy(entry = entry) }
            }
        }
    }

    override fun handleViewEvent(event: EntryViewEvent) {
        return when (event) {
            is OpenHave -> select { PushHave(it) }
            is OpenNeed -> select { PushNeed(it) }
            is OpenNearby -> select { PushNearby(it) }
            is SettingsNavigate -> publish(NavigateToSettings)
        }
    }

    private inline fun select(crossinline func: (entry: FridgeEntry) -> EntryControllerEvent) {
        // TODO(Peter): Bug on initial app load
        // On initial app load, the setState call which loads the entry will happen
        // after this with state call - so the initial page will never load.
        withState {
            entry?.let { e ->
                publish(func(e))
            }
        }
    }

    fun showMenu(visible: Boolean) {
        setState { copy(isSettingsItemVisible = visible) }
    }

    @CheckResult
    fun canShowMap(): Boolean {
        return mapPermission.hasForegroundPermission()
    }
}
