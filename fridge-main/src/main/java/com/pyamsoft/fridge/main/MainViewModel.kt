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
 *
 */

package com.pyamsoft.fridge.main

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.locator.GpsChangeEvent
import com.pyamsoft.fridge.locator.MapPermission
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
    private val bottomBarHeightBus: EventBus<BottomBarHeight>,
    @Named("app_name") appNameRes: Int,
    @Named("debug") debug: Boolean,
    defaultPage: MainPage
) : UiViewModel<MainViewState, MainViewEvent, MainControllerEvent>(
    initialState = MainViewState(
        page = null,
        appNameRes = appNameRes,
        countNeeded = 0,
        countExpiringOrExpired = 0,
        hasNearby = false
    ), debug = debug
) {

    private var versionChecked: Boolean = false

    private val realtimeRunner = highlander<Unit> {
        interactor.listenForItemChanges { handleRealtime(it) }
    }

    init {
        doOnSaveState { state ->
            state.page?.let { p ->
                put(PAGE, p.name)
            }
        }

        doOnInit { savedInstanceState ->
            val savedPageString = savedInstanceState.get<String>(PAGE)
            val page = if (savedPageString == null) defaultPage else {
                MainPage.valueOf(savedPageString)
            }

            selectPage(page)
        }

        doOnInit {
            refreshBadgeCounts()

            viewModelScope.launch(context = Dispatchers.Default) {
                realtimeRunner.call()
            }
        }
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
            is MainViewEvent.OpenHave -> selectPage(MainPage.HAVE)
            is MainViewEvent.OpenNeed -> selectPage(MainPage.NEED)
            is MainViewEvent.OpenCategory -> selectPage(MainPage.CATEGORY)
            is MainViewEvent.OpenNearby -> selectPage(MainPage.NEARBY)
            is MainViewEvent.OpenSettings -> selectPage(MainPage.SETTINGS)
            is MainViewEvent.BottomBarMeasured -> consumeBottomBarHeight(event.height)
        }
    }

    private fun consumeBottomBarHeight(height: Int) {
        viewModelScope.launch(context = Dispatchers.Default) {
            bottomBarHeightBus.send(BottomBarHeight(height))
        }
    }

    fun selectPage(newPage: MainPage) {
        when (newPage) {
            MainPage.NEED -> select(newPage) { MainControllerEvent.PushNeed(it) }
            MainPage.HAVE -> select(newPage) { MainControllerEvent.PushHave(it) }
            MainPage.NEARBY -> select(newPage) { MainControllerEvent.PushNearby(it) }
            MainPage.CATEGORY -> select(newPage) { MainControllerEvent.PushCategory(it) }
            MainPage.SETTINGS -> select(newPage) { MainControllerEvent.PushSettings(it) }
        }
    }

    private fun select(
        newPage: MainPage,
        event: (page: MainPage?) -> (MainControllerEvent)
    ) {
        withState {
            refreshBadgeCounts()

            val oldPage = this.page
            if (oldPage != newPage) {
                Timber.d("Select entry: $newPage")
                setState { copy(page = newPage) }
                withState {
                    Timber.d("Publish selection: $oldPage -> $newPage")
                    publish(event(oldPage))
                }
            } else {
                Timber.w("Selected entry is same page: $newPage")
            }
        }
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
        withState {
            if (!versionChecked) {
                versionChecked = true
                publish(MainControllerEvent.VersionCheck)
            }
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
