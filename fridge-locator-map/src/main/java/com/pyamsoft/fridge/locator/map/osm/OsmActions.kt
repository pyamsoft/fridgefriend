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

package com.pyamsoft.fridge.locator.map.osm

import android.view.ViewGroup
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.fridge.locator.map.databinding.OsmActionsBinding
import com.pyamsoft.pydroid.arch.BindingUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class OsmActions @Inject internal constructor(
    private val owner: LifecycleOwner,
    private val imageLoader: ImageLoader,
    parent: ViewGroup
) : BindingUiView<OsmViewState, OsmViewEvent, OsmActionsBinding>(parent) {

    override val viewBinding by viewBinding(OsmActionsBinding::inflate)

    override val layoutRoot by boundView { osmFindNearby }

    private var boundFindMeImage: Loaded? = null
    private var boundNearbyImage: Loaded? = null

    private var nearbyAnimator: ViewPropertyAnimatorCompat? = null
    private var meAnimator: ViewPropertyAnimatorCompat? = null

    init {
        doOnInflate {
            boundNearbyImage?.dispose()
            boundNearbyImage = imageLoader.load(R.drawable.ic_shopping_cart_24dp)
                .into(binding.osmFindNearby)

            boundFindMeImage?.dispose()
            boundFindMeImage = imageLoader.load(R.drawable.ic_location_search_24dp)
                .into(binding.osmFindMe)

            binding.osmFindNearby.isVisible = false
            binding.osmFindMe.isVisible = false

            binding.osmFindMe.setOnDebouncedClickListener {
                publish(OsmViewEvent.RequestMyLocation(firstTime = false))
            }
            binding.osmFindNearby.setOnDebouncedClickListener {
                publish(OsmViewEvent.RequestFindNearby)
            }
        }

        doOnTeardown {
            binding.osmFindMe.setOnDebouncedClickListener(null)
            binding.osmFindNearby.setOnDebouncedClickListener(null)

            boundFindMeImage?.dispose()
            boundNearbyImage?.dispose()
            boundFindMeImage = null
            boundNearbyImage = null

            dismissNearbyAnimator()
            dismissMeAnimator()
        }
    }

    private fun dismissNearbyAnimator() {
        nearbyAnimator?.cancel()
        nearbyAnimator = null
    }

    private fun dismissMeAnimator() {
        meAnimator?.cancel()
        meAnimator = null
    }

    override fun onRender(state: OsmViewState) {
        state.nearbyError.let { throwable ->
            if (throwable == null) {
                clearError()
            } else {
                showError(throwable)
            }
        }

        state.cachedFetchError.let { throwable ->
            if (throwable == null) {
                clearCacheError()
            } else {
                showCacheError(throwable)
            }
        }

        state.centerMyLocation?.let { event ->
            if (event.firstTime) {
                revealButtons()
            }
        }
    }

    private fun showError(throwable: Throwable) {
        Snackbreak.bindTo(owner, "nearby") {
            make(layoutRoot, throwable.message ?: "An unexpected error occurred.")
        }
    }

    private fun clearError() {
        Snackbreak.bindTo(owner, "nearby") {
            dismiss()
        }
    }

    private fun showCacheError(throwable: Throwable) {
        Snackbreak.bindTo(owner, "cache") {
            make(layoutRoot, throwable.message ?: "An error occurred fetching cached stores.")
        }
    }

    private fun clearCacheError() {
        Snackbreak.bindTo(owner, "cache") {
            dismiss()
        }
    }

    private fun revealButtons() {
        if (nearbyAnimator != null) {
            return
        }
        nearbyAnimator = binding.osmFindNearby.popShow(startDelay = 700L).withEndAction {
            revealFindMeButton()
        }
    }

    private fun revealFindMeButton() {
        dismissNearbyAnimator()
        if (meAnimator != null) {
            return
        }
        meAnimator = binding.osmFindMe.popShow(startDelay = 0L).withEndAction {
            dismissMeAnimator()
        }
    }
}
