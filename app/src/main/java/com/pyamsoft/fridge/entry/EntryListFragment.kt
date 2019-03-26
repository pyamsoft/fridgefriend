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
import com.pyamsoft.fridge.Injector
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.detail.DetailFragment
import com.pyamsoft.fridge.entry.action.EntryActionUiComponent
import com.pyamsoft.fridge.entry.list.EntryListUiComponent
import com.pyamsoft.fridge.entry.toolbar.EntryToolbarUiComponent
import com.pyamsoft.fridge.extensions.fragmentContainerId
import com.pyamsoft.fridge.setting.SettingsFragment
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.util.commit
import timber.log.Timber
import javax.inject.Inject

internal class EntryListFragment : Fragment(),
  EntryListUiComponent.Callback,
  EntryActionUiComponent.Callback,
  EntryToolbarUiComponent.Callback {

  @field:Inject internal lateinit var toolbar: EntryToolbarUiComponent
  @field:Inject internal lateinit var list: EntryListUiComponent
  @field:Inject internal lateinit var action: EntryActionUiComponent

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.layout_coordinator, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val parent = view.findViewById<CoordinatorLayout>(R.id.layout_coordinator)
    Injector.obtain<FridgeComponent>(view.context.applicationContext)
      .plusEntryComponent()
      .toolbarActivity(requireToolbarActivity())
      .parent(parent)
      .build()
      .inject(this)

    list.bind(viewLifecycleOwner, savedInstanceState, this)
    action.bind(viewLifecycleOwner, savedInstanceState, this)
    toolbar.bind(viewLifecycleOwner, savedInstanceState, this)

    action.show()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    list.saveState(outState)
    toolbar.saveState(outState)
    action.saveState(outState)
  }

  private inline fun pushFragment(tag: String, crossinline createFragment: () -> Fragment) {
    val fm = requireActivity().supportFragmentManager
    if (fm.findFragmentByTag(tag) == null) {
      fm.beginTransaction()
        .replace(fragmentContainerId, createFragment(), tag)
        .addToBackStack(null)
        .commit(viewLifecycleOwner)
    }
  }

  override fun onNavigateToSettings() {
    pushFragment(SettingsFragment.TAG) { SettingsFragment.newInstance() }
  }

  override fun onCreateNew(id: String) {
    pushFragment(DetailFragment.TAG) { DetailFragment.newInstance(id) }
  }

  override fun onEditEntry(id: String) {
    pushFragment(DetailFragment.TAG) { DetailFragment.newInstance(id) }
  }

  override fun onStartShopping() {
    Timber.d("TODO: Start shopping")
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
