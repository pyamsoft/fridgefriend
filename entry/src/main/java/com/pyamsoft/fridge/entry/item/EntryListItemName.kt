cd /*
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

package com.pyamsoft.fridge.entry.item

import android.view.ViewGroup
import com.pyamsoft.fridge.entry.databinding.EntryItemNameBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class EntryListItemName @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<EntryItemViewState, EntryItemViewEvent, EntryItemNameBinding>(parent) {

    override val viewBinding = EntryItemNameBinding::inflate

    override val layoutRoot by boundView { entryItemName }

    init {
        doOnInflate {
            layoutRoot.setOnDebouncedClickListener {
                publish(EntryItemViewEvent.ExpandEntry)
            }
        }

        doOnTeardown {
            layoutRoot.setOnDebouncedClickListener(null)
        }

        doOnTeardown {
            clear()
        }
    }

    override fun onRender(state: EntryItemViewState) {
        handleName(state)
    }

    private fun clear() {
        binding.entryItemName.text = ""
    }

    private fun handleName(state: EntryItemViewState) {
        state.entry.name().let { name ->
            if (name.isBlank()) {
                clear()
            } else {
                binding.entryItemName.text = name
            }
        }
    }
}
