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

package com.pyamsoft.fridge.entry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.detail.DetailFragment
import com.pyamsoft.fridge.entry.action.EntryActionControllerEvent.OpenDetails
import com.pyamsoft.fridge.entry.action.EntryActionViewModel
import com.pyamsoft.fridge.entry.action.EntryCreate
import com.pyamsoft.fridge.entry.list.EntryList
import com.pyamsoft.fridge.entry.list.EntryListControllerEvent.OpenForEditing
import com.pyamsoft.fridge.entry.list.EntryListViewModel
import com.pyamsoft.fridge.entry.toolbar.EntryToolbar
import com.pyamsoft.fridge.entry.toolbar.EntryToolbarControllerEvent.NavigateToSettings
import com.pyamsoft.fridge.entry.toolbar.EntryToolbarViewModel
import com.pyamsoft.fridge.extensions.fragmentContainerId
import com.pyamsoft.fridge.setting.SettingsFragment
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.util.commit
import javax.inject.Inject

internal class EntryListFragment : Fragment() {

  @JvmField @Inject internal var toolbar: EntryToolbar? = null
  @JvmField @Inject internal var toolbarViewModel: EntryToolbarViewModel? = null

  @JvmField @Inject internal var createButton: EntryCreate? = null
  @JvmField @Inject internal var actionViewModel: EntryActionViewModel? = null

  @JvmField @Inject internal var list: EntryList? = null
  @JvmField @Inject internal var listViewModel: EntryListViewModel? = null

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

    val parent = view.findViewById<CoordinatorLayout>(R.id.layout_coordinator)
    Injector.obtain<FridgeComponent>(view.context.applicationContext)
        .plusEntryComponent()
        .create(viewLifecycleOwner, parent, requireToolbarActivity())
        .inject(this)

    createComponent(
        savedInstanceState, viewLifecycleOwner,
        requireNotNull(listViewModel),
        requireNotNull(list)
    ) {
      return@createComponent when (it) {
        is OpenForEditing -> navigateToDetails(it.entry)
      }
    }

    createComponent(
        savedInstanceState, viewLifecycleOwner,
        requireNotNull(actionViewModel),
        requireNotNull(createButton)
    ) {
      return@createComponent when (it) {
        is OpenDetails -> navigateToDetails(it.entry)
      }
    }

    createComponent(
        savedInstanceState, viewLifecycleOwner,
        requireNotNull(toolbarViewModel),
        requireNotNull(toolbar)
    ) {
      return@createComponent when (it) {
        is NavigateToSettings -> navigateToSettings()
      }
    }

    requireNotNull(listViewModel).fetchEntries()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    list?.saveState(outState)
    toolbar?.saveState(outState)
    createButton?.saveState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    toolbarViewModel = null
    toolbar = null

    actionViewModel = null
    createButton = null

    listViewModel = null
    list = null
  }

  private inline fun pushFragment(
    tag: String,
    crossinline createFragment: () -> Fragment
  ) {
    val fm = requireActivity().supportFragmentManager
    if (fm.findFragmentByTag(tag) == null) {
      fm.beginTransaction()
          .replace(fragmentContainerId, createFragment(), tag)
          .addToBackStack(null)
          .commit(viewLifecycleOwner)
    }
  }

  private fun navigateToSettings() {
    pushFragment(SettingsFragment.TAG) { SettingsFragment.newInstance() }
  }

  private fun navigateToDetails(entry: FridgeEntry) {
    pushEntryScreen(entry)
  }

  private fun pushEntryScreen(entry: FridgeEntry) {
    pushFragment(DetailFragment.TAG) { DetailFragment.newInstance(entry) }
  }

  override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    toolbarViewModel?.showMenu(!hidden)
  }

  companion object {

    const val TAG = "EntryListFragment"

    @JvmStatic
    @CheckResult
    fun newInstance(): Fragment {
      return EntryListFragment().apply {
        arguments = Bundle().apply {
        }
      }
    }
  }

}
