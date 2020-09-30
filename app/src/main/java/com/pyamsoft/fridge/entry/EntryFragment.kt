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

package com.pyamsoft.fridge.entry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.CheckResult
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailFragment
import com.pyamsoft.fridge.main.VersionChecker
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.viewModelFactory
import com.pyamsoft.pydroid.ui.databinding.LayoutCoordinatorBinding
import com.pyamsoft.pydroid.ui.util.commit
import timber.log.Timber
import javax.inject.Inject

internal class EntryFragment : Fragment(), SnackbarContainer {

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    private val viewModel by viewModelFactory<EntryViewModel> { factory }

    @JvmField
    @Inject
    internal var addNew: EntryAddNew? = null

    @JvmField
    @Inject
    internal var container: EntryContainer? = null

    private var stateSaver: StateSaver? = null

    private var fragmentContainerId = 0

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            childFragmentManager.popBackStack()
        }
    }

    private val backStackChangedListener = FragmentManager.OnBackStackChangedListener {
        val count = childFragmentManager.backStackEntryCount
        val enabled = count > 0
        Timber.d("Back stack callback state: $enabled")
        backPressedCallback.isEnabled = enabled
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_coordinator, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutCoordinatorBinding.bind(view)
        Injector.obtain<FridgeComponent>(view.context.applicationContext)
            .plusEntryComponent()
            .create(viewLifecycleOwner, binding.layoutCoordinator)
            .inject(this)

        val addNew = requireNotNull(addNew)
        val container = requireNotNull(container)

        stateSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            viewModel,
            addNew,
            container,
        ) {
            return@createComponent when (it) {
                is EntryControllerEvent.LoadEntry -> pushPage(it.entry, it.presence)
            }
        }

        initializeApp()
    }

    override fun container(): CoordinatorLayout? {
        val fm = childFragmentManager
        val fragment = fm.findFragmentById(fragmentContainerId)
        return if (fragment is SnackbarContainer) fragment.container() else null
    }

    private fun watchBackPresses() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
        childFragmentManager.addOnBackStackChangedListener(backStackChangedListener)
    }

    private fun initializeApp() {
        watchBackPresses()

        // Set the fragment container ID
        fragmentContainerId = requireNotNull(container).id()

        val act = requireActivity()
        if (act is VersionChecker) {
            act.checkVersionForUpdate()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        stateSaver?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        removeBackWatchers()

        stateSaver = null
        factory = null
        addNew = null
        container = null
    }

    private fun removeBackWatchers() {
        childFragmentManager.removeOnBackStackChangedListener(backStackChangedListener)
    }

    private fun pushPage(entry: FridgeEntry, presence: FridgeItem.Presence) {
        val tag = entry.id().id
        Timber.d("Push new entry page: $tag")
        val fm = childFragmentManager
        if (fm.findFragmentByTag(tag) == null) {
            fm.commit(viewLifecycleOwner) {
                replace(
                    fragmentContainerId,
                    DetailFragment.newInstance(entry, presence),
                    tag
                )
                addToBackStack(null)
            }
        }
    }

    companion object {

        const val TAG = "EntryFragment"

        @JvmStatic
        @CheckResult
        fun newInstance(): Fragment {
            return EntryFragment().apply {
                arguments = Bundle().apply {}
            }
        }
    }
}
