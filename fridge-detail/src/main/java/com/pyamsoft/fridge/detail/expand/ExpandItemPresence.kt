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

package com.pyamsoft.fridge.detail.expand

import android.view.ViewGroup
import com.pyamsoft.fridge.detail.base.BaseItemPresence
import javax.inject.Inject

class ExpandItemPresence @Inject internal constructor(
    parent: ViewGroup
) : BaseItemPresence<ExpandItemViewState, ExpandedItemViewEvent>(parent) {

    init {
        doOnTeardown {
            layoutRoot.handler?.removeCallbacksAndMessages(null)
        }
    }

    override fun onRender(state: ExpandItemViewState) {
        layoutRoot.post { render(state.item) }
    }

    override fun publishChangePresence() {
        publish(ExpandedItemViewEvent.CommitPresence)
    }
}
