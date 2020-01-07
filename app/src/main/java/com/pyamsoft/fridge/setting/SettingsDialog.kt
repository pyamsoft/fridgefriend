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

package com.pyamsoft.fridge.setting

import android.app.Dialog
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.setting.SettingsControllerEvent.NavigateUp
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.UiView
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.about.AboutFragment
import com.pyamsoft.pydroid.ui.arch.factory
import com.pyamsoft.pydroid.ui.util.commit
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.ui.widget.shadow.DropshadowView
import com.pyamsoft.pydroid.util.toDp
import javax.inject.Inject
import timber.log.Timber

internal class SettingsDialog : DialogFragment() {

    @JvmField
    @Inject
    internal var toolbar: SettingToolbar? = null
    @JvmField
    @Inject
    internal var frame: SettingFrame? = null

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    private val viewModel by factory<SettingsViewModel> { factory }

    private var stateSaver: StateSaver? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.layout_constraint, container, false)

        val margin = 16.toDp(view.context)
        val params = view.layoutParams
        if (params == null) {
            view.layoutParams = MarginLayoutParams(margin, margin * 2)
        } else {
            view.updateLayoutParams<MarginLayoutParams> {
                topMargin = margin
                bottomMargin = margin
                marginStart = margin
                marginEnd = margin
            }
        }

        return view
    }

    private fun handleBackPressed() {
        val settingsFragment = childFragmentManager.findFragmentByTag(SettingsFragment.TAG)
        if (settingsFragment != null) {
            val fm = settingsFragment.childFragmentManager
            if (AboutFragment.isPresent(fm)) {
                Timber.d("Handle back pressed")
                fm.popBackStack()
                return
            }
        }

        requireActivity().onBackPressed()
    }

    override fun getTheme(): Int {
        return R.style.Theme_Fridge_Dialog
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(ContextThemeWrapper(requireActivity(), theme), theme) {

            override fun onBackPressed() {
                handleBackPressed()
            }
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {
                    handleBackPressed()
                }
            })

        val parent = view.findViewById<ConstraintLayout>(R.id.layout_constraint)
        Injector.obtain<FridgeComponent>(view.context.applicationContext)
            .plusSettingComponent()
            .create(parent)
            .inject(this)

        val toolbar = requireNotNull(toolbar)
        val dropshadow = DropshadowView.createTyped<SettingsViewState, SettingsViewEvent>(parent)
        val frame = requireNotNull(frame)
        stateSaver = createComponent(
            savedInstanceState, viewLifecycleOwner,
            viewModel,
            frame,
            toolbar,
            dropshadow
        ) {
            return@createComponent when (it) {
                is NavigateUp -> handleBackPressed()
            }
        }

        parent.layout {
            toolbar.also {
                connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }

            dropshadow.also {
                connect(it.id(), ConstraintSet.TOP, toolbar.id(), ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }

            frame.also {
                connect(it.id(), ConstraintSet.TOP, toolbar.id(), ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                connect(
                    it.id(),
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }
        }

        pushSettings(frame)
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.CENTER)
        }
    }

    private fun pushSettings(container: UiView<*, *>) {
        val fm = childFragmentManager
        if (fm.findFragmentByTag(SettingsFragment.TAG) == null) {
            fm.commit(viewLifecycleOwner) {
                add(container.id(), SettingsFragment.newInstance(), SettingsFragment.TAG)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        stateSaver?.saveState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        toolbar = null
        frame = null
        factory = null
        stateSaver = null
    }

    companion object {

        internal const val TAG = "SettingsDialog"
    }
}
