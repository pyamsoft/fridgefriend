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

package com.pyamsoft.fridge.locator.map.osm.popup.base

import android.location.Location
import androidx.annotation.CheckResult
import androidx.lifecycle.viewModelScope
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewModel
import kotlinx.coroutines.launch
import javax.inject.Named

internal abstract class BaseInfoViewModel<T : Any, S : BaseInfoViewState<*>, V : UiViewEvent, C : UiControllerEvent> protected constructor(
    private val interactor: BaseInfoInteractor<T, *, *, *, *, *>,
    initialState: S,
    @Named("debug") debug: Boolean
) : UiViewModel<S, V, C>(initialState, debug) {

    init {
        doOnInit {
            findCachedIfExists()
            listenForRealtime()
        }
    }

    private fun findCachedIfExists() {
        viewModelScope.launch {
            val cached = interactor.getAllCached()
            restoreStateFromCachedData(cached)
        }
    }

    protected fun handleFavoriteAction(
        data: T,
        add: Boolean
    ) {
        viewModelScope.launch {
            if (add) {
                interactor.insertIntoDb(data)
            } else {
                interactor.deleteFromDb(data)
            }
        }
    }

    @CheckResult
    protected fun S.processLocationUpdate(location: Location?): LocationUpdate? {
        // If location hasn't changed, do nothing
        val currentLocation = myLocation
        if (currentLocation == null && location == null) {
            return null
        }

        // If location is cleared, set
        if (currentLocation != null && location == null) {
            return LocationUpdate(location)
        }

        // If location is first fixed, set
        if (currentLocation == null && location != null) {
            return LocationUpdate(location)
        }

        // Require both to be non-null
        requireNotNull(location)
        requireNotNull(currentLocation)

        // A bunch of things can change but we only care about renders around lat and lon
        val latitudeMatch = currentLocation.longitude == location.longitude
        val longitudeMatch = currentLocation.latitude == location.latitude
        return if (latitudeMatch && longitudeMatch) null else LocationUpdate(location)
    }

    abstract fun handleLocationUpdate(location: Location?)

    protected abstract fun listenForRealtime()

    protected abstract fun restoreStateFromCachedData(cached: List<T>)

    protected data class LocationUpdate internal constructor(val location: Location?)
}
