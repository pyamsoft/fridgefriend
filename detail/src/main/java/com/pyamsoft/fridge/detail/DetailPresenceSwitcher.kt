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

import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.databinding.DetailAddNewBinding
import com.pyamsoft.fridge.detail.databinding.DetailPresenceSwitchBinding
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.loader.disposeOnDestroy
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class DetailPresenceSwitcher @Inject internal constructor(
    parent: ViewGroup,
) : BaseUiView<DetailViewState, DetailViewEvent, DetailPresenceSwitchBinding>(parent) {

    override val viewBinding = DetailPresenceSwitchBinding::inflate

    override val layoutRoot by boundView { detailPresenceSwitcherRoot }

    init {
        doOnInflate {
            binding.detailPresenceSwitcherNeed.setOnDebouncedClickListener {
                publish(DetailViewEvent.PresenceSwitched(FridgeItem.Presence.NEED))
            }
        }

        doOnTeardown {
            binding.detailPresenceSwitcherNeed.setOnClickListener(null)
        }

        doOnInflate {
            binding.detailPresenceSwitcherHave.setOnDebouncedClickListener {
                publish(DetailViewEvent.PresenceSwitched(FridgeItem.Presence.HAVE))
            }
        }

        doOnTeardown {
            binding.detailPresenceSwitcherHave.setOnClickListener(null)
        }
    }

    override fun onRender(state: DetailViewState) {
        handlePresence(state)
    }

    private fun handlePresence(state: DetailViewState) {
        state.listItemPresence.let { presence ->
            val isNeed = presence == FridgeItem.Presence.NEED
            val isHave = presence == FridgeItem.Presence.HAVE
                    binding.detailPresenceSwitcherNeed.isEnabled = !isNeed
            binding.detailPresenceSwitcherHave.isEnabled = !isHave
        }
    }

}
