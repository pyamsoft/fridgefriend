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

package com.pyamsoft.fridge.locator.map.popup.base

import android.location.Location
import androidx.annotation.CheckResult
import androidx.lifecycle.viewModelScope
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

abstract class BaseInfoViewModel<T : Any, S : BaseInfoViewState, V : UiViewEvent, C : UiControllerEvent> protected constructor(
    private val interactor: BaseInfoInteractor<T>,
    initialState: S,
) : UiViewModel<S, V, C>(initialState) {

    init {
        findCachedIfExists()
        openRealtime()
    }

    private fun openRealtime() {
        viewModelScope.launch(context = Dispatchers.Default) {
            listenForRealtime()
        }
    }

    private fun findCachedIfExists() {
        viewModelScope.launch(context = Dispatchers.Default) {
            val cached = interactor.getAllCached()
            restoreStateFromCachedData(cached)
        }
    }

    @CheckResult
    private fun S.processLocationUpdate(location: Location?): Location? {
        // If location hasn't changed, do nothing
        val currentLocation = myLocation
        if (currentLocation == null && location == null) {
            return null
        }

        // If location is cleared, set
        if (currentLocation != null && location == null) {
            return location
        }

        // If location is first fixed, set
        if (currentLocation == null && location != null) {
            return location
        }

        // Require both to be non-null
        requireNotNull(location)
        requireNotNull(currentLocation)

        // A bunch of things can change but we only care about renders around lat and lon
        val latitudeMatch = abs(relax(currentLocation.longitude) - relax(location.longitude)) < 1
        val longitudeMatch = abs(relax(currentLocation.latitude) - relax(location.latitude)) < 1
        return if (latitudeMatch && longitudeMatch) null else location
    }

    @CheckResult
    private fun relax(value: Double): Double {
        // Lat and Lon are XXX.YYYYYY values
        // When not moving, XXX.YYYYZZ is common where only the ZZ values are changing.
        // As such, they may be non significant.
        // Decimals out to 6 figures give us centimeter accuracy, we only really care for ~1 meter accuracy
        // which is about 5 decimals out.
        //
        // Multiply the value by 5 decimal places to get the meter significant measurement and then cut it off
        return (abs(value) * 10.0.pow(5))
    }

    private fun restoreStateFromCachedData(cached: List<T>) {
        setState {
            copyState(
                this,
                cached = BaseInfoViewState.Cached(cached = cached.any { isDataMatch(it) })
            )
        }
    }

    private suspend fun listenForRealtime() {
        interactor.listenForNearbyCacheChanges(
            onInsert = { data ->
                if (isDataMatch(data)) {
                    setState { copyState(this, cached = BaseInfoViewState.Cached(true)) }
                }
            },
            onDelete = { data ->
                if (isDataMatch(data)) {
                    setState { copyState(this, cached = BaseInfoViewState.Cached(false)) }
                }
            })
    }

    fun handleLocationUpdate(location: Location?) {
        setState {
            val newLocation = processLocationUpdate(location)
            return@setState if (newLocation == null) this else {
                copyState(this, myLocation = newLocation)
            }
        }
    }

    fun updatePopup(
        name: String,
        latitude: Double,
        longitude: Double
    ) {
        setState {
            copyState(this, name = name, latitude = latitude, longitude = longitude)
        }
    }

    protected fun handleFavoriteAction(
        data: T,
        add: Boolean
    ) {
        viewModelScope.launch(context = Dispatchers.Default) {
            if (add) {
                interactor.insertIntoDb(data)
            } else {
                interactor.deleteFromDb(data)
            }
        }
    }

    @CheckResult
    protected abstract fun isDataMatch(data: T): Boolean

    @CheckResult
    protected abstract fun copyState(
        state: S, myLocation: Location? = state.myLocation,
        cached: BaseInfoViewState.Cached? = state.cached,
        name: String = state.name,
        latitude: Double? = state.latitude,
        longitude: Double? = state.longitude
    ): S
}
