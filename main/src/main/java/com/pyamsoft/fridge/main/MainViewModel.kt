/*
 * Copyright 2020 Peter Kenji Yamanaka
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

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.locator.GpsChangeEvent
import com.pyamsoft.fridge.locator.MapPermission
import com.pyamsoft.fridge.ui.BottomOffset
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.EventBus
import com.pyamsoft.pydroid.arch.UiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class MainViewModel @Inject internal constructor(
    private val interactor: MainInteractor,
    private val mapPermission: MapPermission,
    private val gpsChangeBus: EventBus<GpsChangeEvent>,
    private val bottomOffsetBus: EventBus<BottomOffset>,
    @Named("app_name") appNameRes: Int,
    defaultPage: MainPage
) : UiViewModel<MainViewState, MainViewEvent, MainControllerEvent>(
    MainViewState(
        page = null,
        appNameRes = appNameRes,
        countNeeded = 0,
        countExpiringOrExpired = 0,
        hasNearby = false
    )
) {

    private var versionChecked: Boolean = false

    private val realtimeRunner = highlander<Unit> {
        interactor.listenForItemChanges { handleRealtime(it) }
    }

    init {
        viewModelScope.launch(context = Dispatchers.Default) {
            realtimeRunner.call()
        }

        doOnSaveState { outState, state ->
            state.page?.let { p ->
                outState.put(PAGE, p.name)
            }
        }

        doOnBind { savedInstanceState ->
            val savedPageString = savedInstanceState.get<String>(PAGE)
            val page = if (savedPageString == null) defaultPage else {
                MainPage.valueOf(savedPageString)
            }

            selectPage(page)
        }

        refreshBadgeCounts()
    }

    private fun handleRealtime(event: FridgeItemChangeEvent) {
        return when (event) {
            is FridgeItemChangeEvent.Insert -> refreshBadgeCounts()
            is FridgeItemChangeEvent.Update -> refreshBadgeCounts()
            is FridgeItemChangeEvent.Delete -> refreshBadgeCounts()
        }
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

    override fun handleViewEvent(event: MainViewEvent) {
        return when (event) {
            is MainViewEvent.OpenEntries -> selectPage(MainPage.ENTRIES)
            is MainViewEvent.OpenCategory -> selectPage(MainPage.CATEGORY)
            is MainViewEvent.OpenNearby -> selectPage(MainPage.NEARBY)
            is MainViewEvent.OpenSettings -> selectPage(MainPage.SETTINGS)
            is MainViewEvent.BottomBarMeasured -> consumeBottomBarHeight(event.height)
        }
    }

    private fun consumeBottomBarHeight(height: Int) {
        viewModelScope.launch(context = Dispatchers.Default) {
            bottomOffsetBus.send(BottomOffset(height))
        }
    }

    fun selectPage(newPage: MainPage) {
        when (newPage) {
            MainPage.ENTRIES -> select(newPage) { MainControllerEvent.PushEntry(it) }
            MainPage.NEARBY -> select(newPage) { MainControllerEvent.PushNearby(it) }
            MainPage.CATEGORY -> select(newPage) { MainControllerEvent.PushCategory(it) }
            MainPage.SETTINGS -> select(newPage) { MainControllerEvent.PushSettings(it) }
        }
    }

    private inline fun select(
        newPage: MainPage,
        crossinline event: (page: MainPage?) -> (MainControllerEvent)
    ) {
        Timber.d("Refresh badge counts")
        refreshBadgeCounts()

        val oldPage = state.page
        Timber.d("Select entry: $newPage")
        setState(stateChange = { copy(page = newPage) }, andThen = {
            publishNewSelection(oldPage, event)
        })
    }

    private inline fun publishNewSelection(
        oldPage: MainPage?,
        crossinline event: (page: MainPage?) -> MainControllerEvent
    ) {
        Timber.d("Publish selection: $oldPage -> ${state.page}")
        publish(event(oldPage))
    }

    @JvmOverloads
    fun withForegroundPermission(
        withPermission: () -> Unit = EMPTY,
        withoutPermission: () -> Unit = EMPTY
    ) {
        viewModelScope.launch(context = Dispatchers.Default) {
            if (mapPermission.hasForegroundPermission()) {
                withContext(context = Dispatchers.Main) { withPermission() }
            } else {
                withContext(context = Dispatchers.Main) { withoutPermission() }
            }
        }
    }

    // Called from DetailFragment upon initialization
    fun checkForUpdates() {
        if (!versionChecked) {
            versionChecked = true
            publish(MainControllerEvent.VersionCheck)
        }
    }

    fun publishGpsChange(isEnabled: Boolean) {
        viewModelScope.launch(context = Dispatchers.Default) {
            Timber.d("Publish GPS state change: $isEnabled")
            gpsChangeBus.send(GpsChangeEvent(isEnabled))
        }
    }

    companion object {

        private const val PAGE = "page"
        private val EMPTY = {}
    }
}
