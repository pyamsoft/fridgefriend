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
import androidx.lifecycle.viewModelScope
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.arch.UiViewState
import javax.inject.Named
import kotlinx.coroutines.launch

internal abstract class BaseInfoViewModel<T : Any, S : UiViewState, V : UiViewEvent, C : UiControllerEvent> protected constructor(
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

    abstract fun handleLocationUpdate(location: Location?)

    protected abstract fun listenForRealtime()

    protected abstract fun restoreStateFromCachedData(cached: List<T>)
}
