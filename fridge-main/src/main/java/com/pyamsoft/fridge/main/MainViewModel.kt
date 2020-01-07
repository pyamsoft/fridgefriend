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

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.locator.MapPermission
import com.pyamsoft.pydroid.arch.UiViewModel
import javax.inject.Inject
import javax.inject.Named
import timber.log.Timber

class MainViewModel @Inject internal constructor(
    private val mapPermission: MapPermission,
    @Named("app_name") appNameRes: Int,
    defaultPage: MainPage
) : UiViewModel<MainViewState, MainViewEvent, MainControllerEvent>(
    initialState = MainViewState(
        versionChecked = false,
        page = null,
        isSettingsItemVisible = true,
        appNameRes = appNameRes
    )
) {

    init {
        doOnSaveState { state ->
            state.page?.let { p ->
                putString(PAGE, p.name)
            }
        }

        doOnInit { savedInstanceState ->
            val savedPageString = savedInstanceState?.getString(PAGE)
            val page = if (savedPageString == null) defaultPage else {
                MainPage.valueOf(savedPageString)
            }

            selectPage(page)
        }
    }

    override fun handleViewEvent(event: MainViewEvent) {
        return when (event) {
            is MainViewEvent.OpenHave -> selectPage(MainPage.HAVE)
            is MainViewEvent.OpenNeed -> selectPage(MainPage.NEED)
            is MainViewEvent.OpenNearby -> selectPage(MainPage.NEARBY)
            is MainViewEvent.SettingsNavigate -> publish(MainControllerEvent.NavigateToSettings)
        }
    }

    fun selectPage(page: MainPage) {
        return when (page) {
            MainPage.NEED -> select(
                MainPage.NEED,
                MainControllerEvent.PushNeed
            )
            MainPage.HAVE -> select(
                MainPage.HAVE,
                MainControllerEvent.PushHave
            )
            MainPage.NEARBY -> select(
                MainPage.NEARBY,
                MainControllerEvent.PushNearby
            )
        }
    }

    private fun select(
        page: MainPage,
        event: MainControllerEvent
    ) {
        withState {
            if (this.page != page) {
                Timber.d("Select entry: $page")
                setState { copy(page = page) }

                Timber.d("Publish selection: $event")
                publish(event)
            } else {
                Timber.w("Selected entry is same page: $page")
            }
        }
    }

    // TODO(Peter): Kind of an anti-pattern
    @CheckResult
    fun canShowMap(): Boolean {
        return mapPermission.hasForegroundPermission()
    }

    // Called from DetailFragment upon initialization
    fun checkForUpdates() {
        withState {
            if (!versionChecked) {
                setState { copy(versionChecked = true) }
                publish(MainControllerEvent.VersionCheck)
            }
        }
    }

    companion object {

        private const val PAGE = "page"
    }
}
