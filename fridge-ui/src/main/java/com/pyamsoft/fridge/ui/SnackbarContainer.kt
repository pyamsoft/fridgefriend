/*
 * Copyright 2020 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.ui

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.ui.databinding.SnackbarLayoutBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.pydroid.ui.util.Snackbreak

abstract class SnackbarContainer<S : UiViewState, V : UiViewEvent> protected constructor(
    private val owner: LifecycleOwner,
    parent: ViewGroup
) : BaseUiView<S, V, SnackbarLayoutBinding>(parent) {

    override val layoutRoot by boundView { root }
    override val viewBinding = SnackbarLayoutBinding::inflate

    protected fun makeSnackbar(id: String, message: CharSequence) {
        Snackbreak.bindTo(owner, id) {
            make(binding.snackbarContainer, message)
        }
    }

    protected fun makeSnackbar(id: String, block: Snackbreak.Instance.() -> Unit) {
        Snackbreak.bindTo(owner, id, block)
    }

    protected fun dismissSnackbar(id: String) {
        Snackbreak.bindTo(owner, id) {
            dismiss()
        }
    }
}

