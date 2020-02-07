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

package com.pyamsoft.fridge.detail.expand

import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.detail.base.BaseItemName
import timber.log.Timber
import javax.inject.Inject

class ExpandItemName @Inject internal constructor(
    parent: ViewGroup
) : BaseItemName<ExpandItemViewState, ExpandedItemViewEvent>(parent) {

    private val popupWindow = SimilarlyNamedListWindow(parent.context)
    private var firstRender = true

    init {
        doOnInflate {
            popupWindow.apply {
                initializeView(layoutRoot)
                setOnDismissListener {
                    Timber.d("Similar popup dismissed")
                }
                setOnItemClickListener { selectedItem ->
                    Timber.d("Similar popup FridgeItem selected: $selectedItem")
                    // TODO publish SELECT_SIMILAR event to VM
                    setName(selectedItem)
                }
            }
        }

        doOnTeardown {
            popupWindow.teardown()
        }

        doOnTeardown {
            firstRender = false
        }
    }

    override fun onRender(state: ExpandItemViewState) {
        popupWindow.set(if (nameView.isFocused) state.similarItems else emptyList())

        state.item.let { item ->
            if (item != null) {
                if (firstRender) {
                    firstRender = false
                    setName(item)
                    val watcher = addWatcher()
                    doOnTeardown {
                        removeListeners(watcher)
                    }
                }
            }
        }
    }

    @CheckResult
    private fun addWatcher(): TextWatcher {
        val watcher = object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                s?.also { editable -> commit(editable.toString()) }
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }
        }
        nameView.addTextChangedListener(watcher)
        return watcher
    }

    private fun removeListeners(watcher: TextWatcher) {
        watcher.let { nameView.removeTextChangedListener(it) }
    }

    private fun commit(name: String) {
        publish(ExpandedItemViewEvent.CommitName(name))
    }
}
