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

package com.pyamsoft.fridge.entry.action

import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import com.pyamsoft.fridge.entry.R

internal inline fun View.wiggle(crossinline onAnimationComplete: () -> Unit) {
  val animation = AnimationUtils.loadAnimation(context, R.anim.wiggle).apply {
    setAnimationListener(object : AnimationListener {

      override fun onAnimationRepeat(animation: Animation?) {
      }

      override fun onAnimationEnd(animation: Animation?) {
        onAnimationComplete()
        setAnimationListener(null)
      }

      override fun onAnimationStart(animation: Animation?) {
      }

    })
  }
  startAnimation(animation)
}

