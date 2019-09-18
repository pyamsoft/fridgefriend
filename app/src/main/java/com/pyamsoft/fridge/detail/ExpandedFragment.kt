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
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.JsonMappableFridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import com.pyamsoft.fridge.detail.expand.ExpandItemError
import com.pyamsoft.fridge.detail.expand.ExpandItemSimilar
import com.pyamsoft.fridge.detail.expand.ExpandItemViewModel
import com.pyamsoft.fridge.detail.expand.ExpandedToolbar
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.CloseExpand
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.DatePick
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.ExpandDetails
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent
import com.pyamsoft.fridge.detail.item.DetailItemViewState
import com.pyamsoft.fridge.detail.item.DetailListItemCount
import com.pyamsoft.fridge.detail.item.DetailListItemDate
import com.pyamsoft.fridge.detail.item.DetailListItemName
import com.pyamsoft.fridge.detail.item.DetailListItemPresence
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.factory
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.ui.util.show
import com.pyamsoft.pydroid.ui.widget.shadow.DropshadowView
import timber.log.Timber
import javax.inject.Inject

internal class ExpandedFragment : DialogFragment() {

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null

    @JvmField
    @Inject
    internal var name: DetailListItemName? = null

    @JvmField
    @Inject
    internal var date: DetailListItemDate? = null

    @JvmField
    @Inject
    internal var presence: DetailListItemPresence? = null

    @JvmField
    @Inject
    internal var count: DetailListItemCount? = null

    @JvmField
    @Inject
    internal var errorDisplay: ExpandItemError? = null

    @JvmField
    @Inject
    internal var sameNamedItems: ExpandItemSimilar? = null

    @JvmField
    @Inject
    internal var toolbar: ExpandedToolbar? = null

    private val viewModel by factory<ExpandItemViewModel> { factory }

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
        val itemArgument =
            requireNotNull(requireArguments().getParcelable<JsonMappableFridgeItem>(ITEM))
        val entryArgument =
            requireNotNull(requireArguments().getParcelable<JsonMappableFridgeEntry>(ENTRY))
        val presenceArgument = Presence.valueOf(requireNotNull(requireArguments().getString(PRESENCE)))

        Injector.obtain<FridgeComponent>(view.context.applicationContext)
            .plusExpandComponent()
            .create(parent, itemArgument, entryArgument, presenceArgument)
            .inject(this)

        val name = requireNotNull(name)
        val date = requireNotNull(date)
        val presence = requireNotNull(presence)
        val count = requireNotNull(count)
        val sameNamedItems = requireNotNull(sameNamedItems)
        val errorDisplay = requireNotNull(errorDisplay)
        val toolbar = requireNotNull(toolbar)
        val shadow = DropshadowView.createTyped<DetailItemViewState, DetailItemViewEvent>(parent)
        createComponent(
            null, viewLifecycleOwner,
            viewModel,
            name,
            date,
            presence,
            count,
            sameNamedItems,
            errorDisplay,
            toolbar,
            shadow
        ) {
            return@createComponent when (it) {
                is ExpandDetails -> expandItem(it.item)
                is DatePick -> pickDate(it.oldItem, it.year, it.month, it.day)
                is CloseExpand -> dismiss()
            }
        }

        parent.layout {
            toolbar.also {
                connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            shadow.also {
                connect(it.id(), ConstraintSet.TOP, toolbar.id(), ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }

            errorDisplay.also {
                connect(it.id(), ConstraintSet.TOP, toolbar.id(), ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            sameNamedItems.also {
                connect(it.id(), ConstraintSet.TOP, errorDisplay.id(), ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            val topId = sameNamedItems.id()
            presence.also {
                connect(it.id(), ConstraintSet.TOP, topId, ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            date.also {
                connect(it.id(), ConstraintSet.TOP, topId, ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            name.also {
                connect(it.id(), ConstraintSet.TOP, topId, ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.START, presence.id(), ConstraintSet.END)
                connect(it.id(), ConstraintSet.END, date.id(), ConstraintSet.START)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            count.also {
                connect(it.id(), ConstraintSet.TOP, name.id(), ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                connect(it.id(), ConstraintSet.START, name.id(), ConstraintSet.START)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }
        }
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
        errorDisplay?.saveState(outState)
        toolbar?.saveState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        factory = null
        name = null
        date = null
        presence = null
        toolbar = null
        errorDisplay = null
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
        }
    }

    companion object {

        const val TAG = "ExpandedFragment"

        private const val ITEM = "item"
        private const val ENTRY = "entry"
        private const val PRESENCE = "presence"

        @JvmStatic
        @CheckResult
        fun newInstance(
            entry: FridgeEntry,
            item: FridgeItem,
            presence: Presence
        ): DialogFragment {
            return ExpandedFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ENTRY, JsonMappableFridgeEntry.from(entry))
                    putParcelable(ITEM, JsonMappableFridgeItem.from(item))
                    putString(PRESENCE, presence.name)
                }
            }
        }
    }
}
