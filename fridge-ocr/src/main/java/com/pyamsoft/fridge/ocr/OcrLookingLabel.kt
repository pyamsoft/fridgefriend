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

package com.pyamsoft.fridge.ocr

import android.view.ViewGroup
import android.widget.TextView
import com.pyamsoft.pydroid.arch.BaseUiView
import javax.inject.Inject

class OcrLookingLabel @Inject internal constructor(
  parent: ViewGroup
) : BaseUiView<OcrViewState, OcrViewEvent>(parent) {

  override val layout: Int = R.layout.looking

  override val layoutRoot by boundView<TextView>(R.id.looking)

  override fun onRender(state: OcrViewState) {
    state.text.let { text ->
      layoutRoot.text = text
    }
  }

  override fun onTeardown() {
    layoutRoot.text = ""
  }

}
