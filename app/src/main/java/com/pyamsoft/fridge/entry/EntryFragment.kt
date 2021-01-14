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
import androidx.annotation.CheckResult
import androidx.annotation.IdRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.core.FridgeViewModelFactory
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailFragment
import com.pyamsoft.fridge.entry.create.CreateEntrySheet
import com.pyamsoft.fridge.main.VersionChecker
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.fridge.ui.requireAppBarActivity
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.R
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.arch.fromViewModelFactory
import com.pyamsoft.pydroid.ui.databinding.LayoutCoordinatorBinding
import com.pyamsoft.pydroid.ui.util.commit
import timber.log.Timber
import javax.inject.Inject

internal class EntryFragment : Fragment(), SnackbarContainer {

    @JvmField
    @Inject
    internal var factory: FridgeViewModelFactory? = null
    private val viewModel by fromViewModelFactory<EntryViewModel>(activity = true) {
        factory?.create(requireActivity())
    }

    @JvmField
    @Inject
    internal var container: EntryContainer? = null

    @JvmField
    @Inject
    internal var toolbar: EntryToolbar? = null

    // Nested in container
    @JvmField
    @Inject
    internal var addNew: EntryAddNew? = null

    // Nested in container
    @JvmField
    @Inject
    internal var list: EntryList? = null

    private var stateSaver: StateSaver? = null

    private var fragmentContainerId = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.layout_coordinator, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        fragmentContainerId = requireArguments().getInt(FRAGMENT_CONTAINER, 0)
        val binding = LayoutCoordinatorBinding.bind(view)
        Injector.obtainFromApplication<FridgeComponent>(view.context)
            .plusEntryComponent()
            .create(
                requireAppBarActivity(),
                requireToolbarActivity(),
                requireActivity(),
                viewLifecycleOwner,
                binding.layoutCoordinator
            )
            .inject(this)

        val container = requireNotNull(container)
        val nestedAddNew = requireNotNull(addNew)
        val nestedList = requireNotNull(list)
        container.nest(nestedList, nestedAddNew)

        stateSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            viewModel,
            container,
            requireNotNull(toolbar),
        ) {
            return@createComponent when (it) {
                is EntryControllerEvent.LoadEntry -> pushPage(it.entry, it.presence)
                is EntryControllerEvent.AddEntry -> startAddFlow()
            }
        }

        initializeUpdate()
    }

    private fun initializeUpdate() {
        val act = requireActivity()
        if (act is VersionChecker) {
            act.onVersionCheck()
        }
    }

    private fun pushPage(entry: FridgeEntry, presence: FridgeItem.Presence) {
        pushDetailPage(
            parentFragmentManager,
            viewLifecycleOwner,
            fragmentContainerId,
            entry.id(),
            presence
        )
    }

    private fun startAddFlow() {
        Timber.d("Add new entry")
        CreateEntrySheet.show(requireActivity())
    }

    override fun container(): CoordinatorLayout? {
        return addNew?.container()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        stateSaver?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stateSaver = null
        factory = null
        container = null
        fragmentContainerId = 0
    }

    companion object {

        const val TAG = "EntryFragment"
        private const val FRAGMENT_CONTAINER = "entry_container"

        @JvmStatic
        @CheckResult
        fun newInstance(@IdRes containerId: Int): Fragment {
            return EntryFragment().apply {
                arguments = Bundle().apply {
                    putInt(FRAGMENT_CONTAINER, containerId)
                }
            }
        }

        @JvmStatic
        fun pushDetailPage(
            fragmentManager: FragmentManager,
            owner: LifecycleOwner,
            containerId: Int,
            entryId: FridgeEntry.Id,
            presence: FridgeItem.Presence,
        ) {
            val tag = entryId.id
            Timber.d("Push new entry page: $tag")
            fragmentManager.commit(owner) {
                replace(
                    containerId,
                    DetailFragment.newInstance(entryId, presence),
                    tag
                )
                addToBackStack(null)
            }
        }
    }
}
