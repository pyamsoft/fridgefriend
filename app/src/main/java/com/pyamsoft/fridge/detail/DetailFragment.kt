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
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.list.DetailList
import com.pyamsoft.fridge.detail.list.DetailListControllerEvent.DatePick
import com.pyamsoft.fridge.detail.list.DetailListControllerEvent.ExpandForEditing
import com.pyamsoft.fridge.detail.list.DetailListViewModel
import com.pyamsoft.fridge.detail.title.DetailTitle
import com.pyamsoft.fridge.detail.title.DetailTitleViewModel
import com.pyamsoft.fridge.detail.toolbar.DetailToolbar
import com.pyamsoft.fridge.detail.toolbar.DetailToolbarControllerEvent.EntryArchived
import com.pyamsoft.fridge.detail.toolbar.DetailToolbarControllerEvent.NavigateUp
import com.pyamsoft.fridge.detail.toolbar.DetailToolbarViewModel
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireArguments
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.ui.util.show
import javax.inject.Inject

internal class DetailFragment : Fragment() {

  @JvmField @Inject internal var toolbar: DetailToolbar? = null
  @JvmField @Inject internal var toolbarViewModel: DetailToolbarViewModel? = null

  @JvmField @Inject internal var title: DetailTitle? = null
  @JvmField @Inject internal var titleViewModel: DetailTitleViewModel? = null

  @JvmField @Inject internal var list: DetailList? = null
  @JvmField @Inject internal var listViewModel: DetailListViewModel? = null

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
        .plusDetailComponent()
        .create(
            parent, requireToolbarActivity(), viewLifecycleOwner,
            requireArguments().getString(ENTRY_ID, "")
        )
        .inject(this)

    val list = requireNotNull(list)
    val title = requireNotNull(title)
    val toolbar = requireNotNull(toolbar)

    createComponent(
        savedInstanceState, viewLifecycleOwner,
        requireNotNull(titleViewModel), title
    ) {}

    createComponent(
        savedInstanceState, viewLifecycleOwner,
        requireNotNull(listViewModel), list
    ) {
      return@createComponent when (it) {
        is ExpandForEditing -> expandItem(it.item)
        is DatePick -> pickDate(it.oldItem, it.year, it.month, it.day)
      }
    }

    createComponent(
        savedInstanceState, viewLifecycleOwner,
        requireNotNull(toolbarViewModel), toolbar
    ) {
      return@createComponent when (it) {
        is EntryArchived -> close()
        is NavigateUp -> close()
      }
    }

    parent.layout {
      title.also {
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

      list.also {
        connect(it.id(), ConstraintSet.TOP, title.id(), ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }
    }

    requireNotNull(toolbarViewModel).beginMonitoringEntry()
    requireNotNull(titleViewModel).beginObservingName()
    requireNotNull(listViewModel).fetchItems()
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
    title?.saveState(outState)
    toolbar?.saveState(outState)
    list?.saveState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    listViewModel = null
    list = null

    titleViewModel = null
    title = null

    toolbarViewModel = null
    toolbar = null
  }

  private fun close() {
    requireActivity().onBackPressed()
  }

  private fun expandItem(item: FridgeItem) {
    ExpandedFragment.newInstance(item)
        .show(requireActivity(), ExpandedFragment.TAG)
  }

  companion object {

    const val TAG = "DetailFragment"
    private const val ENTRY_ID = "entry_id"

    @JvmStatic
    @CheckResult
    fun newInstance(id: String): Fragment {
      return DetailFragment().apply {
        arguments = Bundle().apply {
          putString(ENTRY_ID, id)
        }
      }
    }
  }

}
