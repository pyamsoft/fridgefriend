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
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailFragment
import com.pyamsoft.fridge.entry.create.CreateEntrySheet
import com.pyamsoft.fridge.entry.databinding.EntryContainerBinding
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
import com.pyamsoft.pydroid.ui.R as R2

internal class EntryFragment : Fragment(), SnackbarContainer {

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    private val addViewModel by viewModelFactory<EntryAddViewModel>(activity = true) { factory }
    private val listViewModel by viewModelFactory<EntryListViewModel>(activity = true) { factory }

    @JvmField
    @Inject
    internal var list: EntryList? = null

    @JvmField
    @Inject
    internal var addNew: EntryAddNew? = null

    private var stateSaver: StateSaver? = null

    // We add our own fragment container here so that we can replace the entire view
    // in a child transaction
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
        return inflater.inflate(R2.layout.layout_coordinator, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutCoordinatorBinding.bind(view)

        // Inflate a fragment container and add it to the view
        // This is so that we can replace the entire container as a child fragment operation
        val container = EntryContainerBinding.inflate(layoutInflater, binding.layoutCoordinator)
        fragmentContainerId = container.entryContainer.id

        Injector.obtain<FridgeComponent>(view.context.applicationContext)
            .plusEntryComponent()
            .create(
                requireActivity(),
                viewLifecycleOwner,

                // Pass our created container as the parent view for the graph
                // so that child transactions happen on the entire page
                container.entryContainer
            )
            .inject(this)

        val addStateSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            addViewModel,
            requireNotNull(addNew),
        ) {
            return@createComponent when (it) {
                is EntryAddControllerEvent.AddEntry -> startAddFlow()
            }
        }

        val listStateSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            listViewModel,
            requireNotNull(list)
        ) {
            return@createComponent when (it) {
                is EntryControllerEvent.LoadEntry -> pushPage(it.entry, it.presence)
            }
        }

        stateSaver = StateSaver { outState ->
            addStateSaver.saveState(outState)
            listStateSaver.saveState(outState)
        }

        initializeApp()
    }

    private fun startAddFlow() {
        Timber.d("Add new entry")
        CreateEntrySheet.show(requireActivity())
    }

    override fun container(): CoordinatorLayout? {
        return addNew?.container()
    }

    private fun watchBackPresses() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback
        )
        childFragmentManager.addOnBackStackChangedListener(backStackChangedListener)
    }

    private fun initializeApp() {
        watchBackPresses()

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
        list = null
        addNew = null
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
