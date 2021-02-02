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

package com.pyamsoft.fridge.main

import androidx.annotation.CheckResult
import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.ui.BottomOffset
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.arch.UiSavedStateViewModel
import com.pyamsoft.pydroid.arch.UiSavedStateViewModelProvider
import com.pyamsoft.pydroid.bus.EventBus
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Named

class MainViewModel @AssistedInject internal constructor(
    @Assisted savedState: UiSavedState,
    private val interactor: MainInteractor,
    private val bottomOffsetBus: EventBus<BottomOffset>,
    @Named("app_name") appNameRes: Int,
) : UiSavedStateViewModel<MainViewState, MainViewEvent, MainControllerEvent>(
    savedState,
    MainViewState(
        page = null,
        appNameRes = appNameRes,
        countNeeded = 0,
        countExpiringOrExpired = 0,
    )
) {

    init {
        viewModelScope.launch(context = Dispatchers.Default) {
            interactor.listenForItemChanges { handleRealtime(it) }
        }

        refreshBadgeCounts()
    }

    fun loadDefaultPage() {
        viewModelScope.launch(context = Dispatchers.Default) {
            val page = restoreSavedState(KEY_PAGE) { MainPage.Entries.asString() }.asPage()
            Timber.d("Loading initial page: $page")
            selectPage(force = true, page)
        }
    }

    private fun handleRealtime(event: FridgeItemChangeEvent) = when (event) {
        is FridgeItemChangeEvent.Insert -> refreshBadgeCounts()
        is FridgeItemChangeEvent.Update -> refreshBadgeCounts()
        is FridgeItemChangeEvent.Delete -> refreshBadgeCounts()
    }

    private fun refreshBadgeCounts() {
        viewModelScope.launch(context = Dispatchers.Default) {
            val neededCount = interactor.getNeededCount()
            setState { copy(countNeeded = neededCount) }
        }

        viewModelScope.launch(context = Dispatchers.Default) {
            val expiredExpiringCount = interactor.getExpiredOrExpiringCount()
            setState { copy(countExpiringOrExpired = expiredExpiringCount) }
        }
    }

    override fun handleViewEvent(event: MainViewEvent) = when (event) {
        is MainViewEvent.OpenEntries -> selectPage(force = false, MainPage.Entries)
        is MainViewEvent.OpenCategory -> selectPage(force = false, MainPage.Category)
        is MainViewEvent.OpenSettings -> selectPage(force = false, MainPage.Settings)
        is MainViewEvent.OpenSearch -> selectPage(force = false, MainPage.Search)
        is MainViewEvent.BottomBarMeasured -> consumeBottomBarHeight(event.height)
    }

    private fun consumeBottomBarHeight(height: Int) {
        viewModelScope.launch(context = Dispatchers.Default) {
            bottomOffsetBus.send(BottomOffset(height))
        }
    }

    fun selectPage(force: Boolean, page: MainPage) = when (page) {
        is MainPage.Entries -> select(page) { MainControllerEvent.PushEntry(it, force) }
        is MainPage.Category -> select(page) { MainControllerEvent.PushCategory(it, force) }
        is MainPage.Settings -> select(page) { MainControllerEvent.PushSettings(it, force) }
        is MainPage.Search -> select(page) { MainControllerEvent.PushSearch(it, force) }
    }

    private inline fun select(
        newPage: MainPage,
        crossinline event: (page: MainPage?) -> (MainControllerEvent),
    ) {
        Timber.d("Select entry: $newPage")
        refreshBadgeCounts()

        // If the pages match we can just run the after, no need to set and publish
        val oldPage = state.page
        setState(stateChange = { copy(page = newPage) }, andThen = { newState ->
            publishNewSelection(requireNotNull(newState.page), oldPage, event)
        })
    }

    private suspend inline fun publishNewSelection(
        newPage: MainPage,
        oldPage: MainPage?,
        event: (page: MainPage?) -> MainControllerEvent,
    ) {
        Timber.d("Publish selection: $oldPage -> $newPage")
        putSavedState(KEY_PAGE, newPage.asString())
        publish(event(oldPage))
    }

    companion object {

        @CheckResult
        private fun MainPage.asString(): String {
            return this::class.java.name
        }

        @CheckResult
        private fun String.asPage(): MainPage = when (this) {
            MainPage.Entries::class.java.name -> MainPage.Entries
            MainPage.Category::class.java.name -> MainPage.Category
            MainPage.Settings::class.java.name -> MainPage.Settings
            else -> throw IllegalStateException("Cannot convert to MainPage: $this")
        }

        private const val KEY_PAGE = "page"
    }

    @AssistedFactory
    interface Factory : UiSavedStateViewModelProvider<MainViewModel> {
        override fun create(savedState: UiSavedState): MainViewModel
    }

}
