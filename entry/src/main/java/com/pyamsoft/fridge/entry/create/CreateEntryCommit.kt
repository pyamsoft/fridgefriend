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

package com.pyamsoft.fridge.entry.create

import android.view.ViewGroup
import com.pyamsoft.fridge.entry.databinding.CreateEntryCommitBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class CreateEntryCommit @Inject internal constructor(
    parent: ViewGroup,
) : BaseUiView<CreateEntryViewState, CreateEntryViewEvent, CreateEntryCommitBinding>(parent) {

    override val viewBinding = CreateEntryCommitBinding::inflate

    override val layoutRoot by boundView { createEntryCommit }

    init {
        doOnInflate {
            layoutRoot.setOnDebouncedClickListener {
                publish(CreateEntryViewEvent.Commit)
            }
        }

        doOnTeardown {
            layoutRoot.setOnDebouncedClickListener(null)
        }
    }

    private fun handleName(name: String) {
        layoutRoot.isEnabled = name.isNotBlank()
    }

    override fun onRender(state: UiRender<CreateEntryViewState>) {
        state.distinctBy { it.name }.render(viewScope) { handleName(it) }
    }

}
