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
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.pyamsoft.fridge.ui.databinding.SnackbarLayoutBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.pydroid.ui.util.Snackbreak

abstract class SnackbarContainer<S : UiViewState, V : UiViewEvent> protected constructor(
    private val owner: LifecycleOwner,
    parent: ViewGroup
) : BaseUiView<S, V, SnackbarLayoutBinding>(parent) {

    override val layoutRoot by boundView { snackbarRoot }
    override val viewBinding = SnackbarLayoutBinding::inflate

    init {
        doOnInflate { layoutRoot.isVisible = false }
    }

    protected fun addBottomPadding(padding: Int) {
        // layoutRoot.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        //     this.bottomMargin = padding
        // }
        //
        binding.snackbarPadding.updateLayoutParams<ViewGroup.LayoutParams> {
            this.height = padding
        }
    }

    @JvmOverloads
    protected fun makeSnackbar(
        id: String, message: CharSequence,
        onShown: (snackbar: Snackbar) -> Unit = DEFAULT_ON_SHOWN,
        onHidden: (snackbar: Snackbar, event: Int) -> Unit = DEFAULT_ON_HIDDEN,
        builder: Snackbar.() -> Snackbar = DEFAULT_BUILDER
    ) {
        withSnackbar(id) {
            layoutRoot.isVisible = true
            make(
                layoutRoot, message,
                onShown = onShown,
                onHidden = { bar, event ->
                    layoutRoot.isVisible = false
                    onHidden(bar, event)
                },
                builder = builder
            )
        }
    }

    @JvmOverloads
    protected fun shortSnackbar(
        id: String, message: CharSequence,
        onShown: (snackbar: Snackbar) -> Unit = DEFAULT_ON_SHOWN,
        onHidden: (snackbar: Snackbar, event: Int) -> Unit = DEFAULT_ON_HIDDEN,
        builder: Snackbar.() -> Snackbar = DEFAULT_BUILDER
    ) {
        withSnackbar(id) {
            layoutRoot.isVisible = true
            short(
                layoutRoot, message,
                onShown = onShown,
                onHidden = { bar, event ->
                    layoutRoot.isVisible = false
                    onHidden(bar, event)
                },
                builder = builder
            )
        }
    }

    private fun withSnackbar(
        id: String,
        block: Snackbreak.Instance.() -> Unit
    ) {
        Snackbreak.bindTo(owner, id) { block() }
    }

    protected fun dismissSnackbar(id: String) {
        withSnackbar(id) {
            dismiss()
        }
    }

    companion object {
        private val DEFAULT_BUILDER: Snackbar.() -> Snackbar = { this }
        private val DEFAULT_ON_SHOWN: (Snackbar) -> Unit = { }
        private val DEFAULT_ON_HIDDEN: (Snackbar, Int) -> Unit = { _, _ -> }
    }
}

