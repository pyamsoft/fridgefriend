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

package com.pyamsoft.fridge.detail.add

import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.core.view.isVisible
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailViewEvent
import com.pyamsoft.fridge.detail.DetailViewState
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.databinding.AddNewBinding
import com.pyamsoft.pydroid.arch.BindingUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject
import timber.log.Timber

class AddNewItemView @Inject internal constructor(
    private val imageLoader: ImageLoader,
    parent: ViewGroup,
    listItemPresence: FridgeItem.Presence
) : BindingUiView<DetailViewState, DetailViewEvent, AddNewBinding>(parent) {

    override val viewBinding by viewBinding(AddNewBinding::inflate)

    override val layoutRoot by boundView { detailAddNewItem }

    private var addNewIconLoaded: Loaded? = null
    private var filterIconLoaded: Loaded? = null

    private var addNewIconAnimator: ViewPropertyAnimatorCompat? = null
    private var filterIconAnimator: ViewPropertyAnimatorCompat? = null
    private var rotateIconAnimator: ViewPropertyAnimatorCompat? = null

    private var previousRotated = false

    init {
        doOnInflate {
            addNewIconLoaded = imageLoader
                .load(R.drawable.ic_add_24dp)
                .into(binding.detailAddNewItem)

            binding.detailAddNewItem.setOnDebouncedClickListener { expandItem() }
        }

        doOnTeardown {
            disposeAddNewLoaded()
            binding.detailAddNewItem.setOnClickListener(null)
        }

        doOnInflate {
            binding.detailFilterItem.setOnDebouncedClickListener { toggleArchived() }
        }

        doOnInflate {
            val expandAnimator = binding.detailAddNewItem.popShow().withEndAction {
                if (listItemPresence == FridgeItem.Presence.HAVE) {
                    val filterAnimator = binding.detailFilterItem.popShow()
                    doOnTeardown {
                        filterAnimator.cancel()
                    }
                } else {
                    binding.detailFilterItem.isVisible = false
                }
            }
            doOnTeardown {
                expandAnimator.cancel()
            }
        }

        doOnTeardown {
            disposeFilterLoaded()
            binding.detailFilterItem.setOnClickListener(null)
        }

        doOnTeardown {
            disposeAddNewAnimator()
            disposeFilterAnimator()
            disposeRotate()
        }
    }

    private fun disposeFilterAnimator() {
        filterIconAnimator?.cancel()
        filterIconAnimator = null
    }

    private fun disposeFilterLoaded() {
        filterIconLoaded?.dispose()
        filterIconLoaded = null
    }

    private fun disposeAddNewAnimator() {
        addNewIconAnimator?.cancel()
        addNewIconAnimator = null
    }

    private fun disposeAddNewLoaded() {
        addNewIconLoaded?.dispose()
        addNewIconLoaded = null
    }

    private fun disposeRotate() {
        rotateIconAnimator?.cancel()
        rotateIconAnimator = null
    }

    private fun expandItem() {
        publish(DetailViewEvent.AddNewItemEvent)
    }

    private fun toggleArchived() {
        publish(DetailViewEvent.ToggleArchiveVisibility)
    }

    override fun onRender(state: DetailViewState) {
        val presence = state.listItemPresence
        val hideFilter = presence == FridgeItem.Presence.NEED

        state.showing.let { showing ->
            disposeFilterLoaded()
            filterIconLoaded = imageLoader
                .load(
                    when (showing) {
                        DetailViewState.Showing.FRESH -> R.drawable.ic_open_in_browser_24dp
                        DetailViewState.Showing.CONSUMED -> R.drawable.ic_consumed_24dp
                        DetailViewState.Showing.SPOILED -> R.drawable.ic_spoiled_24dp
                    }
                )
                .into(binding.detailFilterItem)
        }

        state.isItemExpanded.let { expanded ->
            Timber.d("Item expanded: $expanded")
            val previous = previousRotated
            previousRotated = expanded
            if (previous != expanded) {
                disposeRotate()
                rotateIconAnimator = ViewCompat.animate(binding.detailAddNewItem).apply {
                    startDelay = 0
                    duration = 200
                    interpolator = ROTATE_INTERPOLATOR

                    // Need to null out the listener here or it will react to above popShow and popHide
                    setListener(null)

                    if (expanded) {
                        rotation(45F)
                    } else {
                        rotation(0F)
                    }
                }
            }
        }

        // Hide filter button for NEED
        if (hideFilter) {
            disposeFilterAnimator()
            binding.detailFilterItem.isVisible = false
        }
    }

    companion object {

        private val ROTATE_INTERPOLATOR = AccelerateInterpolator()
    }
}
