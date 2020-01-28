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

package com.pyamsoft.fridge.detail

import android.content.Context
import android.graphics.Point
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.core.view.isVisible
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.layout
import javax.inject.Inject

class DetailContainer @Inject internal constructor(
    private val emptyState: DetailEmptyState,
    private val list: DetailList,
    parent: ViewGroup
) : BaseUiView<DetailViewState, DetailViewEvent>(parent) {

    override val layout: Int = R.layout.detail_container

    override val layoutRoot by boundView<ConstraintLayout>(R.id.detail_container)

    private var animator: ViewPropertyAnimatorCompat? = null

    init {
        nest(emptyState)
        nest(list)

        doOnTeardown {
            animator?.cancel()
            animator = null
        }

        doOnInflate {
            layoutRoot.layout {
                emptyState.let {
                    connect(
                        it.id(),
                        ConstraintSet.START,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.START
                    )
                    connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                    connect(
                        it.id(),
                        ConstraintSet.BOTTOM,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.BOTTOM
                    )
                    connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                    constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                    constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                }

                list.let {
                    connect(
                        it.id(),
                        ConstraintSet.START,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.START
                    )
                    connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                    connect(
                        it.id(),
                        ConstraintSet.BOTTOM,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.BOTTOM
                    )
                    connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                    constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                    constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                }
            }
        }
    }

    override fun onRender(state: DetailViewState) {
        state.isLoading.let { loading ->
            if (loading != null) {
                // Done loading
                if (!loading.isLoading) {
                    // If root is currently hidden, show it
                    if (!layoutRoot.isVisible) {
                        animateRootIn()
                    }
                }
            }
        }
        emptyState.render(state)
        list.render(state)
    }

    private fun animateRootIn() {
        val root = layoutRoot
        val context = root.context.applicationContext
        if (animator == null) {
            root.post {
                root.translationY = animatingHeight(context)
                root.isVisible = true

                animator = ViewCompat.animate(root)
                    .translationY(0F)
                    .setDuration(600)
                    .setInterpolator(interpolator)
                    .withEndAction {
                        publish(DetailViewEvent.ScrollActionVisibilityChange(true))
                    }
            }
        }
    }

    companion object {

        private val interpolator by lazy(LazyThreadSafetyMode.NONE) { OvershootInterpolator(1.4F) }

        @JvmStatic
        @CheckResult
        private fun animatingHeight(context: Context): Float {
            val point = Point()

            val window =
                requireNotNull(context.applicationContext.getSystemService<WindowManager>())
            window.defaultDisplay.getSize(point)
            return point.y.toFloat()
        }
    }
}