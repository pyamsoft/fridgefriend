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
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.base.FridgeBottomSheetDialogFragment
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import com.pyamsoft.fridge.detail.expand.ExpandItemViewModel
import com.pyamsoft.fridge.detail.item.fridge.DetailItemControllerEvent.CloseExpand
import com.pyamsoft.fridge.detail.item.fridge.DetailItemControllerEvent.DatePick
import com.pyamsoft.fridge.detail.item.fridge.DetailItemControllerEvent.ExpandDetails
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemDate
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemName
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemPresence
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireArguments
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.ui.util.show
import com.pyamsoft.pydroid.util.toDp
import timber.log.Timber
import javax.inject.Inject

class ExpandedFragment : FridgeBottomSheetDialogFragment() {

  @JvmField @Inject internal var viewModel: ExpandItemViewModel? = null
  @JvmField @Inject internal var name: DetailListItemName? = null
  @JvmField @Inject internal var date: DetailListItemDate? = null
  @JvmField @Inject internal var presence: DetailListItemPresence? = null

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
    val horizontalPadding = 16.toDp(parent.context)
    val verticalPadding = 8.toDp(parent.context)
    parent.updatePadding(
        left = horizontalPadding,
        right = horizontalPadding,
        top = verticalPadding,
        bottom = verticalPadding
    )

    val item: FridgeItem =
      requireNotNull(requireArguments().getParcelable<JsonMappableFridgeItem>(ITEM))
    Injector.obtain<FridgeComponent>(view.context.applicationContext)
        .plusExpandComponent()
        .create(parent, item, item.entryId())
        .inject(this)

    val name = requireNotNull(name)
    val date = requireNotNull(date)
    val presence = requireNotNull(presence)
    createComponent(
        null, viewLifecycleOwner,
        requireNotNull(viewModel),
        name,
        date,
        presence
    ) {
      return@createComponent when (it) {
        is ExpandDetails -> expandItem(it.item)
        is DatePick -> pickDate(it.oldItem, it.year, it.month, it.day)
        is CloseExpand -> dismiss()
      }
    }

    parent.layout {
      presence.also {
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
      }

      date.also {
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
      }

      name.also {
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.START, presence.id(), ConstraintSet.END)
        connect(it.id(), ConstraintSet.END, date.id(), ConstraintSet.START)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }
    }

    requireNotNull(viewModel).beginObservingItem()
  }

  private fun expandItem(item: FridgeItem) {
    Timber.d("Noop in expanded fragment: $item")
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
    name?.saveState(outState)
    date?.saveState(outState)
    presence?.saveState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    viewModel = null
    name = null
    date = null
    presence = null
  }

  companion object {

    const val TAG = "ExpandedFragment"
    private const val ITEM = "item"

    @JvmStatic
    @CheckResult
    fun newInstance(
      item: FridgeItem
    ): DialogFragment {
      return ExpandedFragment().apply {
        arguments = Bundle().apply {
          putParcelable(ITEM, JsonMappableFridgeItem.from(item))
        }
      }
    }
  }

}
