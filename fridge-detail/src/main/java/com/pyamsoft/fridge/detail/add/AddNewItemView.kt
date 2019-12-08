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

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Point
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.CheckResult
import androidx.cardview.widget.CardView
import androidx.core.content.getSystemService
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.core.view.ViewPropertyAnimatorListenerAdapter
import com.pyamsoft.fridge.detail.CardAnimator
import com.pyamsoft.fridge.detail.DetailViewEvent
import com.pyamsoft.fridge.detail.DetailViewState
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.tintWith
import com.pyamsoft.pydroid.util.toDp
import javax.inject.Inject

class AddNewItemView @Inject internal constructor(
    private val imageLoader: ImageLoader,
    parent: ViewGroup
) : BaseUiView<DetailViewState, DetailViewEvent>(parent) {

    override val layout: Int = R.layout.add_new

    override val layoutRoot by boundView<CardView>(R.id.detail_add_new_item)
    private val addNewIcon by boundView<ImageView>(R.id.detail_add_new_icon)

    private val filterButton by boundView<CardView>(R.id.detail_filter_item)
    private val filterIcon by boundView<ImageView>(R.id.detail_filter_icon)

    private var addNewIconLoaded: Loaded? = null
    private var filterIconLoaded: Loaded? = null

    private var addNewIconAnimator: ViewPropertyAnimatorCompat? = null
    private var filterIconAnimator: ViewPropertyAnimatorCompat? = null

    private var expandAnimator: ValueAnimator? = null

    private val positionings by lazy(LazyThreadSafetyMode.NONE) {
        Positionings(layoutRoot.context.applicationContext)
    }

    init {
        doOnInflate {
            disposeAddNewLoaded()
            addNewIconLoaded = imageLoader
                .load(R.drawable.ic_add_24dp)
                .mutate { it.tintWith(addNewIcon.context, R.color.white) }
                .into(addNewIcon)

            layoutRoot.setOnDebouncedClickListener { expandItem() }
        }

        doOnTeardown {
            disposeAddNewLoaded()
            layoutRoot.setOnClickListener(null)
        }

        doOnInflate {
            filterButton.setOnDebouncedClickListener {
                publish(DetailViewEvent.ToggleArchiveVisibility)
            }
        }

        doOnTeardown {
            disposeFilterLoaded()
            filterButton.setOnClickListener(null)
        }

        doOnInflate {
            disposeAddNewAnimator()
            addNewIconAnimator =
                layoutRoot.popShow(listener = object : ViewPropertyAnimatorListenerAdapter() {
                    override fun onAnimationEnd(view: View) {
                        disposeFilterAnimator()
                        filterIconAnimator = filterButton.popShow(startDelay = 0)
                    }
                })
        }

        doOnTeardown {
            disposeAddNewAnimator()
            disposeFilterAnimator()
            disposeExpandAnimator()
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

    private fun disposeExpandAnimator() {
        expandAnimator?.cancel()
        expandAnimator = null
    }

    private fun expandItem() {
        disposeExpandAnimator()
        expandAnimator = CardAnimator(
            layoutRoot,
            endX = positionings.addNewMovedX,
            endY = positionings.addNewMovedY,
            endElevation = 12.toDp(layoutRoot.context).toFloat()
        ).animator(true) { progress ->
            if (progress == 1F) {
                publish(DetailViewEvent.AddNewItemEvent)
            }
        }.apply { start() }
    }

    private fun dismissItem() {
        disposeExpandAnimator()
        expandAnimator = CardAnimator(
            layoutRoot,
            endX = positionings.addNewOriginalX,
            endY = positionings.addNewOriginalY,
            endElevation = 6.toDp(layoutRoot.context).toFloat()
        ).animator(true)
            .apply { start() }
    }

    override fun onRender(
        state: DetailViewState,
        savedState: UiSavedState
    ) {
        state.isExpanded.let { expanded ->
            if (!expanded) {
                dismissItem()
            }
        }

        state.showArchived.let { show ->
            disposeFilterLoaded()
            filterIconLoaded = imageLoader
                .load(if (show) R.drawable.ic_add_24dp else R.drawable.ic_date_range_24dp)
                .mutate { it.tintWith(filterButton.context, R.color.white) }
                .into(filterIcon)
        }

        state.actionVisible?.let { action ->
            if (action.visible) {
                disposeAddNewAnimator()
                addNewIconAnimator =
                    layoutRoot.popShow(listener = object : ViewPropertyAnimatorListenerAdapter() {
                        override fun onAnimationEnd(view: View) {
                            disposeFilterAnimator()
                            filterIconAnimator = filterButton.popShow(
                                startDelay = 0,
                                listener = object : ViewPropertyAnimatorListenerAdapter() {
                                    override fun onAnimationEnd(view: View) {
                                        publish(DetailViewEvent.DoneScrollActionVisibilityChange)
                                    }
                                })
                        }
                    })
            } else {
                disposeFilterAnimator()
                filterIconAnimator =
                    filterButton.popShow(listener = object : ViewPropertyAnimatorListenerAdapter() {
                        override fun onAnimationEnd(view: View) {
                            disposeAddNewAnimator()
                            addNewIconAnimator = layoutRoot.popShow(
                                startDelay = 0,
                                listener = object : ViewPropertyAnimatorListenerAdapter() {
                                    override fun onAnimationEnd(view: View) {
                                        publish(DetailViewEvent.DoneScrollActionVisibilityChange)
                                    }
                                })
                        }
                    })
            }
        }
    }

    private class Positionings internal constructor(context: Context) {

        val addNewOriginalX: Float
        val addNewMovedX: Float

        val addNewOriginalY: Float
        val addNewMovedY: Float

        init {
            val screen = screen(context)
            val screenWidth = screenWidth(screen)
            val screenHeight = screenHeight(screen)

            val fabMargin = fabMargin(context)
            val fabSize = fabSize(context)
            val navbarSize = navbarSize(context)

            addNewOriginalX = screenWidth - fabSize - fabMargin
            addNewMovedX = (screenWidth - fabSize) / 2

            val halfMargin = fabMargin / 2
            val y = screenHeight - navbarSize - halfMargin * 4.5F
            addNewOriginalY = y
            addNewMovedY = y / 2 + fabSize
        }

        @CheckResult
        private fun navbarSize(context: Context): Float {
            return 56.toDp(context).toFloat()
        }

        @CheckResult
        private fun fabSize(context: Context): Float {
            return 56.toDp(context).toFloat()
        }

        @CheckResult
        private fun fabMargin(context: Context): Float {
            return 16.toDp(context).toFloat()
        }

        @CheckResult
        private fun screen(context: Context): Point {
            val point = Point()
            val windowService = requireNotNull(context.getSystemService<WindowManager>())
            windowService.defaultDisplay.getSize(point)
            return point
        }

        @CheckResult
        private fun screenWidth(point: Point): Float {
            return point.x.toFloat()
        }

        @CheckResult
        private fun screenHeight(point: Point): Float {
            return point.y.toFloat()
        }
    }
}

