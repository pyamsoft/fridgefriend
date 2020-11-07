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

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.CheckResult
import androidx.appcompat.view.ContextThemeWrapper
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.noTitle
import com.pyamsoft.pydroid.ui.arch.viewModelFactory
import com.pyamsoft.pydroid.ui.databinding.LayoutLinearVerticalBinding
import com.pyamsoft.pydroid.ui.util.show
import com.pyamsoft.pydroid.util.valueFromCurrentTheme
import javax.inject.Inject
import com.pyamsoft.pydroid.ui.R as R2

internal class CreateEntrySheet : BottomSheetDialogFragment() {

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    private val viewModel by viewModelFactory<CreateEntryViewModel> { factory }

    @JvmField
    @Inject
    internal var name: CreateEntryName? = null

    private var stateSaver: StateSaver? = null

    private val themeFromAttrs: Int by lazy(LazyThreadSafetyMode.NONE) {
        requireActivity().valueFromCurrentTheme(R.attr.bottomSheetDialogTheme)
    }

    override fun getTheme(): Int {
        return themeFromAttrs
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return BottomSheetDialog(ContextThemeWrapper(requireActivity(), theme), theme).noTitle()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R2.layout.layout_linear_vertical, container, false)
    }

    @CallSuper
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutLinearVerticalBinding.bind(view)
        Injector.obtain<FridgeComponent>(view.context.applicationContext)
            .plusCreateEntryComponent()
            .create(binding.layoutLinearV)
            .inject(this)

        stateSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            viewModel,
            requireNotNull(name)
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
    }

    companion object {

        private const val TAG = "EntryAddSheet"

        @CheckResult
        private fun newInstance(): DialogFragment {
            return CreateEntrySheet().apply {
                arguments = Bundle().apply {
                }
            }
        }

        fun show(activity: FragmentActivity) {
            newInstance().show(activity, TAG)
        }
    }

}
