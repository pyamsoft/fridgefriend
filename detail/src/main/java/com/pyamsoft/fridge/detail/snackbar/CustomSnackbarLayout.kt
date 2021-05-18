/*
 * Copyright 2021 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.detail.snackbar

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.CheckResult
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorCompat
import com.pyamsoft.fridge.detail.databinding.DetailCustomSnackbarBinding

class CustomSnackbarLayout
internal constructor(
    context: Context,
    attrs: AttributeSet?,
) : LinearLayout(context, attrs), com.google.android.material.snackbar.ContentViewCallback {

  private var textAnimator: ViewPropertyAnimatorCompat? = null
  private var actionAnimator1: ViewPropertyAnimatorCompat? = null
  private var actionAnimator2: ViewPropertyAnimatorCompat? = null

  private var _binding: DetailCustomSnackbarBinding? = null
  val binding: DetailCustomSnackbarBinding
    get() = requireNotNull(_binding)

  override fun onFinishInflate() {
    super.onFinishInflate()
    _binding = DetailCustomSnackbarBinding.bind(this)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    _binding = null

    textAnimator?.cancel()
    textAnimator = null

    actionAnimator1?.cancel()
    actionAnimator2 = null

    actionAnimator2?.cancel()
    actionAnimator2 = null
  }

  @CheckResult
  private fun animateView(
      view: View,
      delay: Int,
      duration: Int,
      animateIn: Boolean,
  ): ViewPropertyAnimatorCompat {
    return view.run {
      alpha = if (animateIn) 0F else 1F

      ViewCompat.animate(this)
          .alpha(if (animateIn) 1F else 0F)
          .setDuration(duration.toLong())
          .setStartDelay(delay.toLong())
          .apply { start() }
    }
  }

  private fun animate(delay: Int, duration: Int, animateIn: Boolean) {
    textAnimator = animateView(binding.snackbarText, delay, duration, animateIn)
    actionAnimator1 = animateView(binding.snackbarAction1, delay, duration, animateIn)
    actionAnimator2 = animateView(binding.snackbarAction2, delay, duration, animateIn)
  }

  override fun animateContentIn(delay: Int, duration: Int) {
    animate(delay, duration, animateIn = true)
  }

  override fun animateContentOut(delay: Int, duration: Int) {
    animate(delay, duration, animateIn = false)
  }
}
