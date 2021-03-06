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

package com.pyamsoft.fridge.ui

import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.CheckResult
import com.pyamsoft.pydroid.loader.ImageTarget
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.loader.Loader

@CheckResult
fun Loader<Drawable>.intoBackground(view: View): Loaded {
  return this.into(
      object : ImageTarget<Drawable> {
        override fun clear() {
          view.background = null
        }

        override fun setImage(image: Drawable) {
          view.background = image
        }
      })
}
