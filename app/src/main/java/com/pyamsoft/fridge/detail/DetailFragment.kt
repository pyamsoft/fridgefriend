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
import com.pyamsoft.fridge.core.FridgeViewModelFactory
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.detail.expand.ExpandedItemDialog
import com.pyamsoft.fridge.ui.R
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.fridge.ui.requireAppBarActivity
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.arch.createSavedStateViewModelFactory
import com.pyamsoft.pydroid.arch.emptyController
import com.pyamsoft.pydroid.arch.newUiController
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.arch.fromViewModelFactory
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
    internal var nestedEmptyState: DetailEmptyState? = null

    // Nested in container
    @JvmField
    @Inject
    internal var nestedList: DetailList? = null

    @JvmField
    @Inject
    internal var switcher: DetailPresenceSwitcher? = null

    @JvmField
    @Inject
    internal var listFactory: DetailListViewModel.Factory? = null
    private val listViewModel by fromViewModelFactory<DetailListViewModel> {
        createSavedStateViewModelFactory(listFactory)
    }

    @JvmField
    @Inject
    internal var addFactory: DetailAddViewModel.Factory? = null
    private val addViewModel by fromViewModelFactory<DetailAddViewModel> {
        createSavedStateViewModelFactory(addFactory)
    }

    @JvmField
    @Inject
    internal var switcherFactory: FridgeViewModelFactory? = null
    private val switcherViewModel by fromViewModelFactory<DetailSwitcherViewModel> {
        switcherFactory?.create(this)
    }

    @JvmField
    @Inject
    internal var toolbarFactory: DetailToolbarViewModel.Factory? = null
    private val toolbarViewModel by fromViewModelFactory<DetailToolbarViewModel> {
        createSavedStateViewModelFactory(toolbarFactory)
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
        val nestedEmptyState = requireNotNull(nestedEmptyState)
        val nestedList = requireNotNull(nestedList)
        container.nest(nestedEmptyState, nestedList)

        val listSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            listViewModel,
            controller = newUiController {
                return@newUiController when (it) {
                    is DetailControllerEvent.ListEvent.ExpandItem -> openExisting(it.item)
                }
            },
            requireNotNull(heroImage),
            container,
        ) {
            return@createComponent when (it) {
                is DetailViewEvent.ListEvent.ChangeItemPresence -> listViewModel.handleCommitPresence(it.index)
                is DetailViewEvent.ListEvent.ConsumeItem -> listViewModel.handleConsume(it.index)
                is DetailViewEvent.ListEvent.DecreaseItemCount -> listViewModel.handleDecreaseCount(it.index)
                is DetailViewEvent.ListEvent.DeleteItem -> listViewModel.handleDelete(it.index)
                is DetailViewEvent.ListEvent.ForceRefresh -> listViewModel.handleRefreshList(true)
                is DetailViewEvent.ListEvent.IncreaseItemCount -> listViewModel.handleIncreaseCount(it.index)
                is DetailViewEvent.ListEvent.RestoreItem -> listViewModel.handleRestore(it.index)
                is DetailViewEvent.ListEvent.SpoilItem -> listViewModel.handleSpoil(it.index)
                is DetailViewEvent.ListEvent.ExpandItem -> listViewModel.handleExpand(it.index)
            }
        }

        val addSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            addViewModel,
            controller = newUiController {
                return@newUiController when(it) {
                    is DetailControllerEvent.AddEvent.AddNew -> createItem(it.entryId, it.presence)
                }
            },
            requireNotNull(addNew),
        ) {
            return@createComponent when (it) {
                is DetailViewEvent.ButtonEvent.AddNew -> addViewModel.handleAddNew()
                is DetailViewEvent.ButtonEvent.AnotherOne -> addViewModel.handleAddAgain(it.item)
                is DetailViewEvent.ButtonEvent.ChangeCurrentFilter -> addViewModel.handleUpdateShowing()
                is DetailViewEvent.ButtonEvent.ClearListError -> addViewModel.handleClearListError()
                is DetailViewEvent.ButtonEvent.ReallyDeleteItemNoUndo -> addViewModel.handleDeleteForever()
                is DetailViewEvent.ButtonEvent.UndoDeleteItem -> addViewModel.handleUndoDelete()
            }
        }

        val switcherSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            switcherViewModel,
            controller = emptyController(),
            requireNotNull(switcher)
        ) {
            return@createComponent when (it) {
                is DetailViewEvent.SwitcherEvent.PresenceSwitched -> switcherViewModel.handlePresenceSwitch(
                    it.presence
                )
            }
        }

        val toolbarSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            toolbarViewModel,
            controller = emptyController(),
            requireNotNull(toolbar)
        ) {
            return@createComponent when (it) {
                is DetailViewEvent.ToolbarEvent.Toolbar.Back -> close()
                is DetailViewEvent.ToolbarEvent.Search.Query -> toolbarViewModel.handleUpdateSearch(
                    it.search
                )
                is DetailViewEvent.ToolbarEvent.Toolbar.ChangeSort -> toolbarViewModel.handleUpdateSort(
                    it.sort
                )
            }
        }

        stateSaver = StateSaver { outState ->
            listSaver.saveState(outState)
            switcherSaver.saveState(outState)
            toolbarSaver.saveState(outState)
            addSaver.saveState(outState)
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

        addFactory = null
        listFactory = null
        switcherFactory = null
        toolbarFactory = null

        heroImage = null
        container = null
        nestedList = null
        nestedEmptyState = null
        addNew = null
        toolbar = null
        switcher = null

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
