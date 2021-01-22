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
import com.pyamsoft.fridge.detail.DetailControllerEvent
import com.pyamsoft.fridge.detail.DetailList
import com.pyamsoft.fridge.detail.DetailPresenceSwitcher
import com.pyamsoft.fridge.detail.DetailSwitcherViewModel
import com.pyamsoft.fridge.detail.expand.ExpandedItemDialog
import com.pyamsoft.fridge.ui.applyToolbarOffset
import com.pyamsoft.fridge.ui.requireAppBarActivity
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.arch.createSavedStateViewModelFactory
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.arch.fromViewModelFactory
import com.pyamsoft.pydroid.ui.databinding.LayoutConstraintBinding
import com.pyamsoft.pydroid.ui.databinding.LayoutCoordinatorBinding
import com.pyamsoft.pydroid.ui.util.layout
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
    internal var search: SearchBarView? = null

    @JvmField
    @Inject
    internal var switcher: DetailPresenceSwitcher? = null

    @JvmField
    @Inject
    internal var factory: FridgeViewModelFactory? = null
    private val factoryFactory by lazy { factory?.create(this) }
    private val listViewModel by fromViewModelFactory<SearchListViewModel> { factoryFactory }
    private val appBarViewModel by fromViewModelFactory<DetailSwitcherViewModel> { factoryFactory }

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
        view.applyToolbarOffset(requireAppBarActivity(), viewLifecycleOwner)

        val entryId = FridgeEntry.Id(requireNotNull(requireArguments().getString(ENTRY)))
        val presence =
            FridgeItem.Presence.valueOf(requireNotNull(requireArguments().getString(PRESENCE)))

        val binding = LayoutCoordinatorBinding.bind(view)
        // Inflate and add a ConstraintLayout here for laying out the later views
        val constraintBinding = LayoutConstraintBinding.inflate(layoutInflater, binding.root, true)
        Injector.obtainFromApplication<FridgeComponent>(view.context)
            .plusSearchComponent()
            .create(
                requireActivity(),
                requireToolbarActivity(),
                requireAppBarActivity(),
                requireActivity(),
                constraintBinding.layoutConstraint,
                viewLifecycleOwner,
                entryId,
                presence
            )
            .inject(this)

        val nestedList = requireNotNull(nestedList)
        val nestedEmptyState = requireNotNull(nestedEmptyState)
        val container = requireNotNull(container)
        val search = requireNotNull(search)
        container.nest(nestedEmptyState, nestedList)

        val searchSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            viewModel,
            search
        ) {
            // Intentionally blank
        }

        val listSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            listViewModel,
            container
        ) {
            return@createComponent when (it) {
                is DetailControllerEvent.Expand.ExpandForEditing -> openExisting(it.item)
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
            searchSaver.saveState(outState)
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

        constraintBinding.layoutConstraint.layout {
            search.let {
                connect(
                    it.id(),
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START
                )
                connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            container.let {
                connect(
                    it.id(),
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START
                )
                connect(it.id(), ConstraintSet.TOP, search.id(), ConstraintSet.BOTTOM)
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

        container = null
        switcher = null
        search = null

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
