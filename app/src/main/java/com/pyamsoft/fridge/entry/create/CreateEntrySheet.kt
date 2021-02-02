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

package com.pyamsoft.fridge.entry.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.CheckResult
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.core.FridgeViewModelFactory
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.R
import com.pyamsoft.pydroid.ui.arch.fromViewModelFactory
import com.pyamsoft.pydroid.ui.databinding.LayoutLinearVerticalBinding
import com.pyamsoft.pydroid.ui.util.show
import javax.inject.Inject

internal class CreateEntrySheet : BottomSheetDialogFragment() {

    @JvmField
    @Inject
    internal var factory: FridgeViewModelFactory? = null
    private val viewModel by fromViewModelFactory<CreateEntryViewModel> { factory?.create(this) }

    @JvmField
    @Inject
    internal var name: CreateEntryName? = null

    @JvmField
    @Inject
    internal var commit: CreateEntryCommit? = null

    private var stateSaver: StateSaver? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_linear_vertical, container, false)
    }

    @CallSuper
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutLinearVerticalBinding.bind(view)
        val entryId = FridgeEntry.Id(requireArguments().getString(ENTRY_ID, ""))
        Injector.obtainFromApplication<FridgeComponent>(view.context)
            .plusCreateEntryComponent()
            .create(binding.layoutLinearV, entryId)
            .inject(this)

        stateSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            viewModel,
            requireNotNull(name),
            requireNotNull(commit)
        ) {
            return@createComponent when (it) {
                is CreateEntryControllerEvent.Commit -> dismiss()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        stateSaver?.saveState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        factory = null
        stateSaver = null
        name = null
        commit = null
    }

    companion object {

        private const val TAG = "EntryAddSheet"
        private const val ENTRY_ID = "entry_id"

        @CheckResult
        private fun newInstance(entry: FridgeEntry?): DialogFragment {
            return CreateEntrySheet().apply {
                arguments = Bundle().apply {
                    entry?.let { putString(ENTRY_ID, it.id().id) }
                }
            }
        }

        fun create(activity: FragmentActivity) {
            newInstance(null).show(activity, TAG)
        }

        fun edit(activity: FragmentActivity, entry: FridgeEntry) {
            newInstance(entry).show(activity, TAG)
        }
    }

}
