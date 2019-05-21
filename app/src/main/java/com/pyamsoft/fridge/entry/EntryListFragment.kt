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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.detail.DetailFragment
import com.pyamsoft.fridge.entry.EntryControllerEvent.NavigateToSettings
import com.pyamsoft.fridge.entry.EntryControllerEvent.PushHave
import com.pyamsoft.fridge.entry.EntryControllerEvent.PushNeed
import com.pyamsoft.fridge.extensions.fragmentContainerId
import com.pyamsoft.fridge.setting.SettingsFragment
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.util.commit
import com.pyamsoft.pydroid.ui.util.layout
import javax.inject.Inject

internal class EntryListFragment : Fragment() {

  @JvmField @Inject internal var toolbar: EntryToolbar? = null
  @JvmField @Inject internal var frame: EntryFrame? = null
  @JvmField @Inject internal var navigation: EntryNavigation? = null
  @JvmField @Inject internal var viewModel: EntryViewModel? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.layout_constraint, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val parent = view.findViewById<ConstraintLayout>(R.id.layout_constraint)
    Injector.obtain<FridgeComponent>(view.context.applicationContext)
        .plusEntryComponent()
        .create(viewLifecycleOwner, parent, requireToolbarActivity())
        .inject(this)

    val toolbar = requireNotNull(toolbar)
    val frame = requireNotNull(frame)
    val navigation = requireNotNull(navigation)

    createComponent(
        savedInstanceState, viewLifecycleOwner,
        requireNotNull(viewModel),
        frame,
        toolbar,
        navigation
    ) {
      return@createComponent when (it) {
        is PushHave -> pushHave(it.entry)
        is PushNeed -> pushNeed(it.entry)
        is NavigateToSettings -> navigateToSettings()
      }
    }

    parent.layout {
      navigation.also {
        connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
      }

      frame.also {
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.BOTTOM, navigation.id(), ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }
    }

    requireNotNull(viewModel).fetchEntries()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    frame?.saveState(outState)
    toolbar?.saveState(outState)
    navigation?.saveState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    viewModel = null
    toolbar = null
    frame = null
    navigation = null
  }

  private fun navigateToSettings() {
    val fm = requireActivity().supportFragmentManager
    if (fm.findFragmentByTag(SettingsFragment.TAG) == null) {
      fm.beginTransaction()
          .replace(fragmentContainerId, SettingsFragment.newInstance(), SettingsFragment.TAG)
          .addToBackStack(null)
          .commit(viewLifecycleOwner)
    }
  }

  private fun pushHave(entry: FridgeEntry) {
    pushPage(entry, HAVE)
  }

  private fun pushNeed(entry: FridgeEntry) {
    pushPage(entry, NEED)
  }

  private fun pushPage(
    entry: FridgeEntry,
    filterPresence: Presence
  ) {
    val fm = childFragmentManager
    if (fm.findFragmentByTag(filterPresence.name) == null) {
      fm.beginTransaction()
          .replace(
              requireNotNull(frame).id(),
              DetailFragment.newInstance(entry, filterPresence),
              filterPresence.name
          )
          .commit(viewLifecycleOwner)
    }
  }

  override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    viewModel?.showMenu(!hidden)
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
