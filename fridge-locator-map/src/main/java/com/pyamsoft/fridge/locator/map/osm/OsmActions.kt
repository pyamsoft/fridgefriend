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

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.core.view.ViewPropertyAnimatorListenerAdapter
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
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
) : BaseUiView<OsmViewState, OsmViewEvent>(parent), LifecycleObserver {

    override val layout: Int = R.layout.osm_actions

    override val layoutRoot by boundView<ViewGroup>(R.id.osm_action_frame)

    private var boundFindMeImage: Loaded? = null
    private var boundNearbyImage: Loaded? = null
    private var boundBackgroundImage: Loaded? = null

    private val findNearby by boundView<FloatingActionButton>(R.id.osm_find_nearby)
    private val findMe by boundView<FloatingActionButton>(R.id.osm_find_me)
    private val backgroundPermission by boundView<FloatingActionButton>(
        R.id.osm_background_location_permission
    )

    private var nearbyAnimator: ViewPropertyAnimatorCompat? = null
    private var meAnimator: ViewPropertyAnimatorCompat? = null
    private var backgroundAnimator: ViewPropertyAnimatorCompat? = null

    init {
        doOnInflate {
            owner.lifecycle.addObserver(this)

            boundNearbyImage?.dispose()
            boundNearbyImage = imageLoader.load(R.drawable.ic_shopping_cart_24dp)
                .into(findNearby)

            boundFindMeImage?.dispose()
            boundFindMeImage = imageLoader.load(R.drawable.ic_location_search_24dp)
                .into(findMe)

            boundBackgroundImage?.dispose()
            boundBackgroundImage = imageLoader.load(R.drawable.ic_location_24dp)
                .into(backgroundPermission)

            findNearby.isVisible = false
            findMe.isVisible = false
            backgroundPermission.isVisible = false

            findMe.setOnDebouncedClickListener {
                publish(OsmViewEvent.RequestMyLocation(firstTime = false))
            }
            backgroundPermission.setOnDebouncedClickListener {
                publish(OsmViewEvent.RequestBackgroundPermission)
            }
            findNearby.setOnDebouncedClickListener {
                publish(OsmViewEvent.RequestFindNearby)
            }
        }

        doOnTeardown {
            owner.lifecycle.removeObserver(this)

            findMe.setOnDebouncedClickListener(null)
            findNearby.setOnDebouncedClickListener(null)
            backgroundPermission.setOnDebouncedClickListener(null)

            boundFindMeImage?.dispose()
            boundNearbyImage?.dispose()
            boundBackgroundImage?.dispose()
            boundFindMeImage = null
            boundNearbyImage = null
            boundBackgroundImage = null

            dismissBackgroundAnimator()
            dismissNearbyAnimator()
            dismissMeAnimator()
        }
    }

    private fun dismissBackgroundAnimator() {
        backgroundAnimator?.cancel()
        backgroundAnimator = null
    }

    private fun dismissNearbyAnimator() {
        nearbyAnimator?.cancel()
        nearbyAnimator = null
    }

    private fun dismissMeAnimator() {
        meAnimator?.cancel()
        meAnimator = null
    }

    override fun onRender(
        state: OsmViewState,
        savedState: UiSavedState
    ) {
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
                revealButtons(state.hasBackgroundPermission)
            }
        }

        state.hasBackgroundPermission.let {
            dismissBackgroundAnimator()
            backgroundPermission.isVisible = false
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

    private fun revealButtons(hasBackgroundPermission: Boolean) {
        dismissNearbyAnimator()
        nearbyAnimator = findNearby.popShow(
            startDelay = 700L,
            listener = object : ViewPropertyAnimatorListenerAdapter() {
                override fun onAnimationEnd(view: View) {
                    dismissMeAnimator()
                    meAnimator = findMe.popShow(
                        startDelay = 0,
                        listener = object : ViewPropertyAnimatorListenerAdapter() {
                            override fun onAnimationEnd(view: View) {
                                if (!hasBackgroundPermission) {
                                    dismissBackgroundAnimator()
                                    backgroundAnimator =
                                        backgroundPermission.popShow(startDelay = 0)
                                }
                            }
                        })
                }
            })
    }
}
