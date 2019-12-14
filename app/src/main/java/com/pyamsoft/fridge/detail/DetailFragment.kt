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

package com.pyamsoft.fridge.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.JsonMappableFridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.detail.add.AddNewItemView
import com.pyamsoft.fridge.main.SnackbarContainer
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.arch.factory
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.show
import javax.inject.Inject

internal class DetailFragment : Fragment(), SnackbarContainer {

    @JvmField
    @Inject
    internal var list: DetailList? = null

    @JvmField
    @Inject
    internal var addNew: AddNewItemView? = null

    @JvmField
    @Inject
    internal var background: DetailBackground? = null

    @JvmField
    @Inject
    internal var theming: Theming? = null

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    private val viewModel by factory<DetailViewModel> { factory }

    private var rootView: ViewGroup? = null

    override fun getSnackbarContainer(): ViewGroup? {
        return rootView
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_coordinator, container, false)
    }

    @CheckResult
    private fun getEntryArgument(): FridgeEntry {
        return requireNotNull(requireArguments().getParcelable<JsonMappableFridgeEntry>(ENTRY))
    }

    @CheckResult
    private fun getPresenceArgument(): Presence {
        return Presence.valueOf(requireNotNull(requireArguments().getString(PRESENCE)))
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val parent = view.findViewById<CoordinatorLayout>(R.id.layout_coordinator)
        rootView = parent
        Injector.obtain<FridgeComponent>(view.context.applicationContext)
            .plusDetailComponent()
            .create(
                ThemeProvider { requireNotNull(theming).isDarkTheme(requireActivity()) },
                parent, requireToolbarActivity(), viewLifecycleOwner,
                getEntryArgument(), getPresenceArgument()
            )
            .inject(this)

        val list = requireNotNull(list)
        val addNew = requireNotNull(addNew)
        val background = requireNotNull(background)

        createComponent(
            savedInstanceState, viewLifecycleOwner,
            viewModel,
            background,
            list,
            addNew
        ) {
            return@createComponent when (it) {
                is DetailControllerEvent.ExpandForEditing -> expandItem(it.item)
                is DetailControllerEvent.EntryArchived -> close()
                is DetailControllerEvent.AddNew -> expandItem(FridgeItem.create(entryId = it.entryId))
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        list?.saveState(outState)
        addNew?.saveState(outState)
        background?.saveState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        rootView = null
        factory = null
        background = null
        list = null
        addNew = null
    }

    private fun close() {
        requireActivity().onBackPressed()
    }

    private fun expandItem(item: FridgeItem) {
        ExpandedFragment.newInstance(getEntryArgument(), item, getPresenceArgument())
            .show(requireActivity(), ExpandedFragment.TAG)
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
                    putParcelable(ENTRY, JsonMappableFridgeEntry.from(entry))
                    putString(PRESENCE, filterPresence.name)
                }
            }
        }
    }
}
