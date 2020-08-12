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

package com.pyamsoft.fridge.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.detail.expand.ExpandedFragment
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.arch.viewModelFactory
import com.pyamsoft.pydroid.ui.databinding.LayoutCoordinatorBinding
import com.pyamsoft.pydroid.ui.util.show
import javax.inject.Inject

internal class DetailFragment : Fragment(), SnackbarContainer {

    @JvmField
    @Inject
    internal var container: DetailContainer? = null

    @JvmField
    @Inject
    internal var addNew: DetailAddItemView? = null

    @JvmField
    @Inject
    internal var heroImage: DetailHeroImage? = null

    @JvmField
    @Inject
    internal var toolbar: DetailToolbar? = null

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    private val viewModel by viewModelFactory<DetailViewModel> { factory }

    private var stateSaver: StateSaver? = null

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

        val entryId = FridgeEntry.Id(requireNotNull(requireArguments().getString(ENTRY)))
        val presence = Presence.valueOf(requireNotNull(requireArguments().getString(PRESENCE)))

        val binding = LayoutCoordinatorBinding.bind(view)
        Injector.obtain<FridgeComponent>(view.context.applicationContext)
            .plusDetailComponent()
            .create(
                requireActivity(),
                binding.layoutCoordinator,
                requireToolbarActivity(),
                viewLifecycleOwner,
                entryId,
                presence
            )
            .inject(this)

        val container = requireNotNull(container)
        val addNew = requireNotNull(addNew)
        val heroImage = requireNotNull(heroImage)
        val toolbar = requireNotNull(toolbar)

        stateSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            viewModel,
            heroImage,
            container,
            addNew,
            toolbar
        ) {
            return@createComponent when (it) {
                is DetailControllerEvent.ExpandForEditing -> openExisting(it.item)
                is DetailControllerEvent.EntryArchived -> close()
                is DetailControllerEvent.AddNew -> createItem(it.id, it.presence)
            }
        }
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

        factory = null
        heroImage = null
        container = null
        addNew = null
        toolbar = null
        stateSaver = null
    }

    private fun close() {
        requireActivity().onBackPressed()
    }

    private fun createItem(entryId: FridgeEntry.Id, presence: Presence) {
        showExpandDialog(ExpandedFragment.createNew(entryId, presence))
    }

    private fun openExisting(item: FridgeItem) {
        showExpandDialog(ExpandedFragment.openExisting(item))
    }

    private fun showExpandDialog(dialogFragment: DialogFragment) {
        dialogFragment.show(requireActivity(), ExpandedFragment.TAG)
    }

    companion object {

        private const val ENTRY = "entry"
        private const val PRESENCE = "presence"

        @JvmStatic
        @CheckResult
        fun newInstance(
            entry: FridgeEntry,
            filterPresence: Presence
        ): Fragment {
            return DetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ENTRY, entry.id().id)
                    putString(PRESENCE, filterPresence.name)
                }
            }
        }
    }
}
