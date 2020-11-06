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
 */

package com.pyamsoft.fridge.ui.view

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.ui.databinding.UiEditTextBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

abstract class UiEditText<S : UiViewState, V : UiViewEvent> protected constructor(
    parent: ViewGroup
) : BaseUiView<S, V, UiEditTextBinding>(parent) {

    // NOTE(Peter): Hack because Android does not allow us to use Controlled view components like
    // React does by binding input and drawing to the render loop.
    //
    // This initialRenderPerformed variable allows us to set the initial state of a view once, and bind listeners to
    // it because the state.item is only available in render instead of inflate. Once the firstRender
    // has set the view component up, the actual input will no longer be tracked via state render events,
    // so the input is uncontrolled.
    private var initialRenderPerformed = false

    override val viewBinding = UiEditTextBinding::inflate

    final override val layoutRoot: View by boundView { uiEditTextContainer }

    init {
        doOnTeardown {
            clear()
        }

        doOnInflate {
            if (isWatchingForTextChanges) {
                val watcher = createTextWatcher()
                binding.uiEditText.apply {
                    addTextChangedListener(watcher)
                    doOnTeardown {
                        removeTextChangedListener(watcher)
                    }
                }
            }
        }
    }

    protected abstract val isWatchingForTextChanges: Boolean

    @CheckResult
    protected open fun createTextChangedEvent(text: String): V {
        throw IllegalStateException("If you are watching for text changes, you must provide a text change event")
    }

    /**
     * True if first render was performed, false if not.
     */
    @CheckResult
    protected open fun onFirstRender(state: S): Boolean {
        return true
    }

    @CheckResult
    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {

            private var oldText = ""

            override fun afterTextChanged(s: Editable) {
                val newText = s.toString()
                if (newText != oldText) {
                    oldText = newText
                    publish(createTextChangedEvent(newText))
                }
            }

            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
            }
        }
    }

    @CallSuper
    override fun onRender(state: S) {
        if (!initialRenderPerformed) {
            if (onFirstRender(state)) {
                initialRenderPerformed = true
            }
        }
    }

    protected fun setText(text: String) {
        binding.uiEditText.setTextKeepState(text)
    }

    protected fun clear() {
        binding.uiEditText.text.clear()
    }
}
