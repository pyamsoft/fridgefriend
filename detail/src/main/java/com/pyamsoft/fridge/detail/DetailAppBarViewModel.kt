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

package com.pyamsoft.fridge.detail

import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.arch.UiSavedStateViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class DetailAppBarViewModel @AssistedInject internal constructor(
    @DetailInternalApi delegate: DetailListStateModel,
    @Assisted savedState: UiSavedState,
) : DelegatedDetailViewModel<DetailViewEvent.SwitcherEvent, Nothing>(savedState, delegate) {

    override fun handleViewEvent(event: DetailViewEvent.SwitcherEvent) = when (event) {
        is DetailViewEvent.SwitcherEvent.PresenceSwitched -> delegate.handlePresenceSwitch(event.presence)
    }

    @AssistedFactory
    interface Factory : UiSavedStateViewModelProvider<DetailAppBarViewModel> {
        override fun create(savedState: UiSavedState): DetailAppBarViewModel
    }
}

