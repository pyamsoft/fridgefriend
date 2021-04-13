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

package com.pyamsoft.fridge.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.core.FridgeViewModelFactory
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailList
import com.pyamsoft.fridge.detail.DetailPresenceSwitcher
import com.pyamsoft.fridge.detail.DetailSwitcherViewModel
import com.pyamsoft.fridge.detail.DetailViewEvent
import com.pyamsoft.fridge.detail.expand.ExpandedItemDialog
import com.pyamsoft.fridge.ui.requireAppBarActivity
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.arch.createSavedStateViewModelFactory
import com.pyamsoft.pydroid.arch.emptyController
import com.pyamsoft.pydroid.arch.newUiController
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.fromViewModelFactory
import com.pyamsoft.pydroid.ui.databinding.LayoutCoordinatorBinding
import com.pyamsoft.pydroid.ui.util.show
import javax.inject.Inject
import com.pyamsoft.pydroid.ui.R as R2

internal class SearchFragment : Fragment() {

    @JvmField
    @Inject
    internal var container: SearchContainer? = null

    @JvmField
    @Inject
    internal var nestedEmptyState: SearchEmptyState? = null

    @JvmField
    @Inject
    internal var nestedList: DetailList? = null

    @JvmField
    @Inject
    internal var spacer: SearchAppbarSpacer? = null

    @JvmField
    @Inject
    internal var search: SearchBarView? = null

    @JvmField
    @Inject
    internal var filter: SearchFilter? = null

    @JvmField
    @Inject
    internal var switcher: DetailPresenceSwitcher? = null

    @JvmField
    @Inject
    internal var factory: FridgeViewModelFactory? = null
    private val appBarViewModel by fromViewModelFactory<DetailSwitcherViewModel> {
        factory?.create(this)
    }

    @JvmField
    @Inject
    internal var listViewFactory: SearchListViewModel.Factory? = null
    private val listViewModel by fromViewModelFactory<SearchListViewModel> {
        createSavedStateViewModelFactory(listViewFactory)
    }


    @JvmField
    @Inject
    internal var filterViewFactory: SearchFilterViewModel.Factory? = null
    private val filterViewModel by fromViewModelFactory<SearchFilterViewModel> {
        createSavedStateViewModelFactory(filterViewFactory)
    }

    @JvmField
    @Inject
    internal var searchFactory: SearchViewModel.Factory? = null
    private val viewModel by fromViewModelFactory<SearchViewModel> {
        createSavedStateViewModelFactory(searchFactory)
    }

    private var stateSaver: StateSaver? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R2.layout.layout_coordinator, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val entryId = FridgeEntry.Id(requireNotNull(requireArguments().getString(ENTRY)))
        val presence =
            FridgeItem.Presence.valueOf(requireNotNull(requireArguments().getString(PRESENCE)))

        val binding = LayoutCoordinatorBinding.bind(view)
        Injector.obtainFromApplication<FridgeComponent>(view.context)
            .plusSearchComponent()
            .create(
                requireActivity(),
                requireAppBarActivity(),
                requireActivity(),
                binding.layoutCoordinator,
                viewLifecycleOwner,
                entryId,
                presence
            )
            .inject(this)

        val nestedList = requireNotNull(nestedList)
        val nestedEmptyState = requireNotNull(nestedEmptyState)
        val container = requireNotNull(container)
        container.nest(nestedEmptyState, nestedList)

        val searchSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            viewModel,
            controller = emptyController(),
            requireNotNull(search)
        ) {
            return@createComponent when (it) {
                is DetailViewEvent.ToolbarEvent.Search.Query -> viewModel.handleUpdateSearch(it.search)
            }
        }

        val appBarSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            appBarViewModel,
            controller = emptyController(),
            requireNotNull(switcher)
        ) {
            return@createComponent when (it) {
                is DetailViewEvent.SwitcherEvent.PresenceSwitched -> appBarViewModel.handlePresenceSwitch(
                    it.presence
                )
            }
        }


        val listSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            listViewModel,
            controller = newUiController {
                return@newUiController when (it) {
                    is SearchControllerEvent.ExpandItem -> openExisting(it.item)
                }
            },
            requireNotNull(spacer),
            container,
        ) {
            return@createComponent when (it) {
                is DetailViewEvent.ListEvent.ChangeItemPresence -> listViewModel.handleCommitPresence(
                    it.index
                )
                is DetailViewEvent.ListEvent.ConsumeItem -> listViewModel.handleConsume(it.index)
                is DetailViewEvent.ListEvent.DecreaseItemCount -> listViewModel.handleDecreaseCount(
                    it.index
                )
                is DetailViewEvent.ListEvent.DeleteItem -> listViewModel.handleDelete(it.index)
                is DetailViewEvent.ListEvent.ExpandItem -> listViewModel.handleExpand(it.index)
                is DetailViewEvent.ListEvent.ForceRefresh -> listViewModel.handleRefreshList(true)
                is DetailViewEvent.ListEvent.IncreaseItemCount -> listViewModel.handleIncreaseCount(
                    it.index
                )
                is DetailViewEvent.ListEvent.RestoreItem -> listViewModel.handleRestore(it.index)
                is DetailViewEvent.ListEvent.SpoilItem -> listViewModel.handleSpoil(it.index)
            }
        }

        val filterSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            filterViewModel,
            controller = emptyController(),
            requireNotNull(filter)
        ) {
            return@createComponent when (it) {
                is SearchViewEvent.AnotherOne -> filterViewModel.handleAddAgain(it.item)
                is SearchViewEvent.ChangeCurrentFilter -> filterViewModel.handleUpdateShowing()
                is SearchViewEvent.ReallyDeleteItemNoUndo -> filterViewModel.handleDeleteForever()
                is SearchViewEvent.UndoDeleteItem -> filterViewModel.handleUndoDelete()
            }
        }

        stateSaver = StateSaver { outState ->
            listSaver.saveState(outState)
            appBarSaver.saveState(outState)
            searchSaver.saveState(outState)
            filterSaver.saveState(outState)
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

    override fun onSaveInstanceState(outState: Bundle) {
        stateSaver?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        factory = null
        filterViewFactory = null
        listViewFactory = null
        searchFactory = null

        filter = null
        container = null
        switcher = null
        search = null
        spacer = null

        nestedList = null
        nestedEmptyState = null

        stateSaver = null
    }

    private fun openExisting(item: FridgeItem) {
        ExpandedItemDialog.openExisting(item).show(requireActivity(), ExpandedItemDialog.TAG)
    }

    companion object {

        internal const val TAG = "SearchFragment"
        private const val ENTRY = "entry"
        private const val PRESENCE = "presence"

        @JvmStatic
        @CheckResult
        fun newInstance(): Fragment {
            return SearchFragment().apply {
                arguments = Bundle().apply {
                    putString(ENTRY, FridgeEntry.Id.EMPTY.id)
                    putString(PRESENCE, FridgeItem.Presence.NEED.name)
                }
            }
        }
    }
}
