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
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.fridge.locator.map.databinding.OsmActionsBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class OsmActions @Inject internal constructor(
    private val imageLoader: ImageLoader,
    parent: ViewGroup
) : BaseUiView<OsmViewState, OsmViewEvent, OsmActionsBinding>(parent) {

    override val viewBinding = OsmActionsBinding::inflate

    override val layoutRoot by boundView { osmFindNearby }

    private var boundFindMeImage: Loaded? = null
    private var boundNearbyImage: Loaded? = null

    private var nearbyAnimator: ViewPropertyAnimatorCompat? = null
    private var meAnimator: ViewPropertyAnimatorCompat? = null

    private var originalNearbyItemBottomMargin = 0
    private var originalMeItemBottomMargin = 0

    init {
        doOnInflate {
            binding.osmFindNearby.isVisible = false
            binding.osmFindMe.isVisible = false
        }

        doOnInflate {
            boundNearbyImage?.dispose()
            boundNearbyImage = imageLoader.load(R.drawable.ic_shopping_cart_24dp)
                .into(binding.osmFindNearby)
        }

        doOnInflate {
            boundFindMeImage?.dispose()
            boundFindMeImage = imageLoader.load(R.drawable.ic_location_search_24dp)
                .into(binding.osmFindMe)
        }

        doOnInflate {
            binding.osmFindMe.setOnDebouncedClickListener {
                publish(OsmViewEvent.RequestMyLocation(firstTime = false))
            }
        }

        doOnInflate {
            binding.osmFindNearby.setOnDebouncedClickListener {
                publish(OsmViewEvent.RequestFindNearby)
            }
        }

        doOnInflate {
            binding.osmFindNearby.post {
                originalNearbyItemBottomMargin = binding.osmFindNearby.marginBottom
            }

            binding.osmFindMe.post {
                originalMeItemBottomMargin = binding.osmFindMe.marginBottom
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
        layoutRoot.post { handleCenterLocation(state) }
        layoutRoot.post { handleBottomMargin(state) }
    }

    private fun handleBottomMargin(state: OsmViewState) {
        state.bottomOffset.let { height ->
            if (height > 0) {
                if (originalNearbyItemBottomMargin > 0) {
                    binding.osmFindNearby.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        bottomMargin = originalNearbyItemBottomMargin + height
                    }
                }

                if (originalMeItemBottomMargin > 0) {
                    binding.osmFindMe.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        bottomMargin = originalMeItemBottomMargin + height
                    }
                }
            }
        }
    }

    private fun handleCenterLocation(state: OsmViewState) {
        state.centerMyLocation?.let { event ->
            if (event.firstTime) {
                revealButtons()
            }
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
