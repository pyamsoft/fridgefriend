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

package com.pyamsoft.fridge.ui.view

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.annotation.CheckResult

class UiEditTextDelegate(
    private val view: EditText,
    private val watcher: UiTextWatcher
) {

    // NOTE(Peter): Hack because Android does not allow us to use Controlled view components like
    // React does by binding input and drawing to the render loop.
    //
    // This initialRenderPerformed variable allows us to set the initial state of a view once, and bind listeners to
    // it because the state.item is only available in render instead of inflate. Once the firstRender
    // has set the view component up, the actual input will no longer be tracked via state render events,
    // so the input is uncontrolled.
    private var initialRenderPerformed = false

    private var textWatcher: TextWatcher? = null

    private fun killWatcher() {
        textWatcher?.also { view.removeTextChangedListener(it) }
        textWatcher = null
    }

    @CheckResult
    private fun createTextWatcher(): TextWatcher {
        return object : TextWatcher {

            private var oldText = ""

            override fun afterTextChanged(s: Editable) {
                val newText = s.toString()
                val previousText = oldText
                if (newText != previousText) {
                    oldText = newText
                    watcher.onTextChanged(previousText, newText)
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

    fun create() {
        killWatcher()
        val watcher = createTextWatcher()
        view.addTextChangedListener(watcher)
    }

    fun destroy() {
        killWatcher()
        clear()
    }

    fun render(text: String) {
        if (initialRenderPerformed) {
            return;
        }

        initialRenderPerformed = true
        if (text.isNotBlank()) {
            setText(text)
        }
    }

    fun setText(text: String) {
        view.setTextKeepState(text)
    }

    fun clear() {
        view.text.clear()
    }

}
