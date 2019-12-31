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
import com.pyamsoft.fridge.core.DefaultActivityPage
import com.pyamsoft.fridge.db.PersistentEntries
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.EntryControllerEvent.AppInitialized
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
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class EntryViewModel @Inject internal constructor(
    private val mapPermission: MapPermission,
    private val persistentEntries: PersistentEntries,
    @Named("app_name") appNameRes: Int,
    defaultPage: DefaultActivityPage
) : UiViewModel<EntryViewState, EntryViewEvent, EntryControllerEvent>(
    initialState = EntryViewState(
        appInitialized = false,
        page = defaultPage,
        isSettingsItemVisible = true,
        appNameRes = appNameRes
    )
) {

    init {
        doOnSaveState { state ->
            putString(PAGE, state.page.name)
        }

        doOnSaveState { state ->
            putBoolean(INITIALIZED, state.appInitialized)
        }

        doOnInit { savedInstanceState ->
            val savedPageString = savedInstanceState?.getString(PAGE)
            val page = if (savedPageString == null) defaultPage else {
                DefaultActivityPage.valueOf(savedPageString)
            }

            setState { copy(page = page) }
            pushPage(page)
        }

        doOnInit { savedInstanceState ->
            val initialized = savedInstanceState?.getBoolean(INITIALIZED, false) ?: false
            if (!initialized) {
                setState { copy(appInitialized = true) }
                withState {
                    if (appInitialized) {
                        publish(AppInitialized)
                    }
                }
            }
        }
    }

    override fun handleViewEvent(event: EntryViewEvent) {
        return when (event) {
            is OpenHave -> pushPage(DefaultActivityPage.HAVE)
            is OpenNeed -> pushPage(DefaultActivityPage.NEED)
            is OpenNearby -> pushPage(DefaultActivityPage.NEARBY)
            is SettingsNavigate -> publish(NavigateToSettings)
        }
    }

    private fun pushPage(page: DefaultActivityPage) {
        return when (page) {
            DefaultActivityPage.NEED -> select(DefaultActivityPage.NEED) { PushNeed(it) }
            DefaultActivityPage.HAVE -> select(DefaultActivityPage.HAVE) { PushHave(it) }
            DefaultActivityPage.NEARBY -> select(DefaultActivityPage.NEARBY) { PushNearby }
        }
    }

    private inline fun select(
        page: DefaultActivityPage,
        crossinline func: (entry: FridgeEntry) -> EntryControllerEvent
    ) {
        Timber.d("Select entry")
        setState { copy(page = page) }

        viewModelScope.launch(context = Dispatchers.Default) {
            val entry = persistentEntries.getPersistentEntry()

            val event = func(entry)
            Timber.d("Publish selection: $event")
            publish(event)
        }
    }

    // TODO(Peter): Kind of an anti-pattern
    @CheckResult
    fun canShowMap(): Boolean {
        return mapPermission.hasForegroundPermission()
    }

    companion object {

        private const val PAGE = "page"
        private const val INITIALIZED = "initialized"
    }
}
