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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.JsonMappableFridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.detail.DetailControllerEvent.DatePick
import com.pyamsoft.fridge.detail.DetailControllerEvent.EntryArchived
import com.pyamsoft.fridge.detail.DetailControllerEvent.ExpandForEditing
import com.pyamsoft.fridge.detail.DetailControllerEvent.NavigateUp
import com.pyamsoft.fridge.detail.add.AddNewControllerEvent.AddNew
import com.pyamsoft.fridge.detail.add.AddNewItemView
import com.pyamsoft.fridge.detail.add.AddNewItemViewModel
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireArguments
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.ui.util.show
import javax.inject.Inject

internal class DetailFragment : Fragment() {

  @JvmField @Inject internal var factory: ViewModelProvider.Factory? = null

  @JvmField @Inject internal var list: DetailList? = null
  private var viewModel: DetailViewModel? = null

  @JvmField @Inject internal var addNew: AddNewItemView? = null
  private var addNewViewModel: AddNewItemViewModel? = null

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.layout_constraint, container, false)
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

    val parent = view.findViewById<ConstraintLayout>(R.id.layout_constraint)
    Injector.obtain<FridgeComponent>(view.context.applicationContext)
        .plusDetailComponent()
        .create(
            requireActivity(), parent, requireToolbarActivity(),
            viewLifecycleOwner, getEntryArgument(), getPresenceArgument()
        )
        .inject(this)

    ViewModelProviders.of(this, factory)
        .let { factory ->
          viewModel = factory.get(DetailViewModel::class.java)
          addNewViewModel = factory.get(AddNewItemViewModel::class.java)
        }

    val list = requireNotNull(list)
    val addNew = requireNotNull(addNew)

    createComponent(
        savedInstanceState, viewLifecycleOwner,
        requireNotNull(viewModel),
        list
    ) {
      return@createComponent when (it) {
        is ExpandForEditing -> expandItem(it.item)
        is DatePick -> pickDate(it.oldItem, it.year, it.month, it.day)
        is EntryArchived -> close()
        is NavigateUp -> close()
      }
    }

    createComponent(
        savedInstanceState, viewLifecycleOwner,
        requireNotNull(addNewViewModel),
        addNew
    ) {
      return@createComponent when (it) {
        is AddNew -> expandItem(FridgeItem.create(entryId = it.entryId))
      }
    }

    parent.layout {

      list.also {
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

      addNew.also {
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
      }
    }
  }

  private fun pickDate(
    oldItem: FridgeItem,
    year: Int,
    month: Int,
    day: Int
  ) {
    DatePickerDialogFragment.newInstance(oldItem, year, month, day)
        .show(requireActivity(), DatePickerDialogFragment.TAG)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    list?.saveState(outState)
    addNew?.saveState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    viewModel = null
    addNewViewModel = null
    list = null
    addNew = null
    factory = null
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
