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
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.detail.DetailFragment
import com.pyamsoft.fridge.entry.EntryControllerEvent.NavigateToSettings
import com.pyamsoft.fridge.entry.EntryControllerEvent.PushHave
import com.pyamsoft.fridge.entry.EntryControllerEvent.PushNearby
import com.pyamsoft.fridge.entry.EntryControllerEvent.PushNeed
import com.pyamsoft.fridge.locator.MapFragment
import com.pyamsoft.fridge.locator.MapPermission
import com.pyamsoft.fridge.locator.PermissionFragment
import com.pyamsoft.fridge.main.SnackbarContainer
import com.pyamsoft.fridge.setting.SettingsDialog
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.arch.factory
import com.pyamsoft.pydroid.ui.util.commitNow
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.ui.util.show
import com.pyamsoft.pydroid.ui.version.VersionCheckActivity
import com.pyamsoft.pydroid.ui.widget.shadow.TopshadowView
import timber.log.Timber
import javax.inject.Inject

internal class EntryFragment : Fragment(), SnackbarContainer {

  @JvmField @Inject internal var factory: ViewModelProvider.Factory? = null
  @JvmField @Inject internal var mapPermission: MapPermission? = null

  @JvmField @Inject internal var toolbar: EntryToolbar? = null
  @JvmField @Inject internal var frame: EntryFrame? = null
  @JvmField @Inject internal var navigation: EntryNavigation? = null
  private val viewModel by factory<EntryViewModel> { factory }

  private var initialized = false

  override fun getSnackbarContainer(): ViewGroup? {
    val frame = frame ?: return null

    val fragment = childFragmentManager.findFragmentById(frame.id())
    if (fragment is SnackbarContainer) {
      return fragment.getSnackbarContainer()
    }

    return null
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initialized = savedInstanceState?.getBoolean(INITIALIZED, false) ?: false
  }

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
    val topshadow = TopshadowView.createTyped<EntryViewState, EntryViewEvent>(parent)

    createComponent(
        savedInstanceState, viewLifecycleOwner,
        viewModel,
        frame,
        navigation,
        toolbar,
        topshadow
    ) {
      return@createComponent when (it) {
        is PushHave -> pushHave(it.entry)
        is PushNeed -> pushNeed(it.entry)
        is PushNearby -> pushNearby()
        is NavigateToSettings -> showSettingsDialog()
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
        connect(it.id(), ConstraintSet.BOTTOM, navigation.id(), ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

      topshadow.also {
        connect(it.id(), ConstraintSet.BOTTOM, navigation.id(), ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }
    }
  }

  private fun onAppInitialized() {
    if (!initialized) {
      initialized = true
      val activity = requireActivity()
      if (activity is VersionCheckActivity) {
        Timber.d("Trigger update check")
        activity.checkForUpdate()
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(INITIALIZED, initialized)
    frame?.saveState(outState)
    toolbar?.saveState(outState)
    navigation?.saveState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    factory = null
    toolbar = null
    frame = null
    navigation = null
    mapPermission = null
  }

  private fun showSettingsDialog() {
    SettingsDialog().show(requireActivity(), SettingsDialog.TAG)
  }

  private fun pushHave(entry: FridgeEntry) {
    pushPage(entry, HAVE)
  }

  private fun pushNeed(entry: FridgeEntry) {
    pushPage(entry, NEED)
  }

  private fun pushNearby() {
    val fm = childFragmentManager
    if (
        fm.findFragmentByTag(MapFragment.TAG) == null &&
        fm.findFragmentByTag(PermissionFragment.TAG) == null
    ) {
      fm.commitNow(viewLifecycleOwner) {
        val container = requireNotNull(frame).id()
        if (requireNotNull(mapPermission).hasForegroundPermission()) {
          replace(container, MapFragment.newInstance(), MapFragment.TAG)
        } else {
          replace(container, PermissionFragment.newInstance(container), PermissionFragment.TAG)
        }
      }
    }

    onAppInitialized()
  }

  private fun pushPage(
    entry: FridgeEntry,
    filterPresence: Presence
  ) {
    val fm = childFragmentManager
    if (fm.findFragmentByTag(filterPresence.name) == null) {
      fm.commitNow(viewLifecycleOwner) {
        replace(
            requireNotNull(frame).id(),
            DetailFragment.newInstance(entry, filterPresence),
            filterPresence.name
        )
      }
    }

    onAppInitialized()

  }

  override fun onHiddenChanged(hidden: Boolean) {
    super.onHiddenChanged(hidden)
    viewModel.showMenu(!hidden)
  }

  companion object {

    private const val INITIALIZED = "initialized"
    const val TAG = "EntryFragment"

    @JvmStatic
    @CheckResult
    fun newInstance(): Fragment {
      return EntryFragment().apply {
        arguments = Bundle().apply {
        }
      }
    }
  }

}
