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
import androidx.constraintlayout.widget.ConstraintSet
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.core.createAssistedFactory
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.detail.expand.ExpandedItemDialog
import com.pyamsoft.fridge.ui.R
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.fridge.ui.requireAppBarActivity
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.arch.viewModelFactory
import com.pyamsoft.pydroid.ui.databinding.LayoutCoordinatorBinding
import com.pyamsoft.pydroid.ui.util.show
import javax.inject.Inject
import com.pyamsoft.pydroid.ui.R as R2

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

    // Nested in container
    @JvmField
    @Inject
    internal var emptyState: DetailEmptyState? = null

    // Nested in container
    @JvmField
    @Inject
    internal var list: DetailList? = null


    @JvmField
    @Inject
    internal var switcher: DetailPresenceSwitcher? = null

    @JvmField
    @Inject
    internal var factory: DetailViewModel.Factory? = null
    private val viewModel by viewModelFactory<DetailViewModel> { createAssistedFactory(factory) }

    @JvmField
    @Inject
    internal var appBarFactory: DetailAppBarViewModel.Factory? = null
    private val appBarViewModel by viewModelFactory<DetailAppBarViewModel> {
        createAssistedFactory(appBarFactory)
    }

    private var stateSaver: StateSaver? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R2.layout.layout_coordinator, container, false).apply {
            // Cover the existing Entry List
            setBackgroundResource(R.color.windowBackground)
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val entryId = FridgeEntry.Id(requireNotNull(requireArguments().getString(ENTRY)))
        val presence = Presence.valueOf(requireNotNull(requireArguments().getString(PRESENCE)))

        val binding = LayoutCoordinatorBinding.bind(view)
        Injector.obtainFromApplication<FridgeComponent>(view.context)
            .plusDetailComponent()
            .create(
                requireActivity(),
                requireToolbarActivity(),
                requireAppBarActivity(),
                requireActivity(),
                binding.layoutCoordinator,
                viewLifecycleOwner,
                entryId,
                presence
            )
            .inject(this)

        val container = requireNotNull(container)
        val nestedEmptyState = requireNotNull(emptyState)
        val nestedList = requireNotNull(list)
        container.nest(nestedEmptyState, nestedList)

        val listSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            viewModel,
            requireNotNull(heroImage),
            container,
            requireNotNull(addNew),
            requireNotNull(toolbar)
        ) {
            return@createComponent when (it) {
                is DetailControllerEvent.ExpandForEditing -> openExisting(it.item)
                is DetailControllerEvent.EntryArchived -> close()
                is DetailControllerEvent.AddNew -> createItem(it.id, it.presence)
                is DetailControllerEvent.Back -> close()
            }
        }

        val appBarSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            appBarViewModel,
            requireNotNull(switcher)
        )
        {
            // Intentionally blank
        }

        stateSaver = StateSaver { outState ->
            listSaver.saveState(outState)
            appBarSaver.saveState(outState)
        }

        container.layout {
            nestedEmptyState.let {
                connect(
                    it.id(),
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START
                )
                connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(
                    it.id(),
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }

            nestedList.let {
                connect(
                    it.id(),
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START
                )
                connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(
                    it.id(),
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
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
        appBarFactory = null
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
        showExpandDialog(ExpandedItemDialog.createNew(entryId, presence))
    }

    private fun openExisting(item: FridgeItem) {
        showExpandDialog(ExpandedItemDialog.openExisting(item))
    }

    private fun showExpandDialog(dialogFragment: DialogFragment) {
        dialogFragment.show(requireActivity(), ExpandedItemDialog.TAG)
    }

    companion object {

        private const val ENTRY = "entry"
        private const val PRESENCE = "presence"

        @JvmStatic
        @CheckResult
        fun newInstance(
            entryId: FridgeEntry.Id,
            filterPresence: Presence,
        ): Fragment {
            return DetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ENTRY, entryId.id)
                    putString(PRESENCE, filterPresence.name)
                }
            }
        }
    }
}
