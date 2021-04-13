/*
 * Copyright 2021 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.detail.expand

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.DialogFragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.detail.expand.date.DateSelectDialog
import com.pyamsoft.fridge.detail.expand.move.ItemMoveDialog
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.UiController
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.arch.createSavedStateViewModelFactory
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.makeFullWidth
import com.pyamsoft.pydroid.ui.arch.fromViewModelFactory
import com.pyamsoft.pydroid.ui.databinding.LayoutConstraintBinding
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.ui.util.show
import com.pyamsoft.pydroid.ui.widget.shadow.DropshadowView
import timber.log.Timber
import javax.inject.Inject
import com.pyamsoft.pydroid.ui.R as R2

internal class ExpandedItemDialog : AppCompatDialogFragment(),
    UiController<ExpandedControllerEvent> {

    @JvmField
    @Inject
    internal var name: ExpandItemName? = null

    @JvmField
    @Inject
    internal var date: ExpandItemDate? = null

    @JvmField
    @Inject
    internal var purchased: ExpandItemPurchasedDate? = null

    @JvmField
    @Inject
    internal var count: ExpandItemCount? = null

    @JvmField
    @Inject
    internal var errorDisplay: ExpandItemError? = null

    @JvmField
    @Inject
    internal var sameNamedItems: ExpandItemSimilar? = null

    @JvmField
    @Inject
    internal var presence: ExpandItemPresence? = null

    @JvmField
    @Inject
    internal var toolbar: ExpandItemToolbar? = null

    @JvmField
    @Inject
    internal var categories: ExpandItemCategoryList? = null

    private var stateSaver: StateSaver? = null

    @JvmField
    @Inject
    internal var factory: ExpandItemViewModel.Factory? = null
    private val viewModel by fromViewModelFactory<ExpandItemViewModel> {
        createSavedStateViewModelFactory(factory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R2.layout.layout_constraint, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        makeFullWidth()

        val binding = LayoutConstraintBinding.bind(view)
        val itemId = FridgeItem.Id(requireNotNull(requireArguments().getString(ITEM)))
        val entryId = FridgeEntry.Id(requireNotNull(requireArguments().getString(ENTRY)))
        val presenceArgument =
            Presence.valueOf(requireNotNull(requireArguments().getString(PRESENCE)))

        Injector.obtainFromApplication<FridgeComponent>(view.context)
            .plusExpandComponent()
            .create(
                requireActivity(),
                requireActivity(),
                binding.layoutConstraint,
                viewLifecycleOwner,
                itemId,
                entryId,
                presenceArgument
            )
            .inject(this)

        val name = requireNotNull(name)
        val date = requireNotNull(date)
        val presence = requireNotNull(presence)
        val count = requireNotNull(count)
        val sameNamedItems = requireNotNull(sameNamedItems)
        val errorDisplay = requireNotNull(errorDisplay)
        val toolbar = requireNotNull(toolbar)
        val categories = requireNotNull(categories)
        val purchased = requireNotNull(purchased)
        val shadow =
            DropshadowView.createTyped<ExpandedViewState, ExpandedViewEvent>(binding.layoutConstraint)

        stateSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            viewModel,
            this,
            name,
            date,
            presence,
            count,
            sameNamedItems,
            errorDisplay,
            categories,
            purchased,
            toolbar,
            shadow
        ) {
            return@createComponent when (it) {
                is ExpandedViewEvent.ItemEvent.CommitCategory -> viewModel.handleCommitCategory(it.index)
                is ExpandedViewEvent.ItemEvent.CommitCount -> viewModel.handleCommitCount(it.count)
                is ExpandedViewEvent.ItemEvent.CommitName -> viewModel.handleCommitName(it.name)
                is ExpandedViewEvent.ItemEvent.CommitPresence -> viewModel.handleCommitPresence()
                is ExpandedViewEvent.ItemEvent.PickDate -> viewModel.handlePickDate()
                is ExpandedViewEvent.ItemEvent.SelectSimilar -> viewModel.handlePickSimilar(it.item)
                is ExpandedViewEvent.ToolbarEvent.CloseItem -> viewModel.handleCloseSelf()
                is ExpandedViewEvent.ToolbarEvent.ConsumeItem -> viewModel.handleConsumeSelf()
                is ExpandedViewEvent.ToolbarEvent.DeleteItem -> viewModel.handleDeleteSelf()
                is ExpandedViewEvent.ToolbarEvent.MoveItem -> viewModel.handleMoveItem()
                is ExpandedViewEvent.ToolbarEvent.RestoreItem -> viewModel.handleRestoreSelf()
                is ExpandedViewEvent.ToolbarEvent.SpoilItem -> viewModel.handleSpoilSelf()
            }
        }

        binding.layoutConstraint.layout {
            toolbar.also {
                connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
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

            presence.also {
                connect(it.id(), ConstraintSet.TOP, sameNamedItems.id(), ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.BOTTOM, name.id(), ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            date.also {
                connect(it.id(), ConstraintSet.TOP, sameNamedItems.id(), ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.BOTTOM, name.id(), ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            name.also {
                connect(it.id(), ConstraintSet.TOP, sameNamedItems.id(), ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.START, presence.id(), ConstraintSet.END)
                connect(it.id(), ConstraintSet.END, date.id(), ConstraintSet.START)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            count.also {
                connect(it.id(), ConstraintSet.TOP, name.id(), ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                connect(it.id(), ConstraintSet.START, name.id(), ConstraintSet.START)

                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            purchased.also {
                connect(it.id(), ConstraintSet.TOP, count.id(), ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                connect(it.id(), ConstraintSet.START, name.id(), ConstraintSet.START)

                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            categories.also {
                connect(it.id(), ConstraintSet.TOP, purchased.id(), ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }
        }
    }

    override fun onControllerEvent(event: ExpandedControllerEvent) {
        return when (event) {
            is ExpandedControllerEvent.Close -> dismiss()
            is ExpandedControllerEvent.DatePicked -> pickDate(
                event.oldItem,
                event.year,
                event.month,
                event.day
            )
            is ExpandedControllerEvent.MoveItem -> loadMoveSubDialog(event.item)
        }
    }

    private fun loadMoveSubDialog(item: FridgeItem) {
        ItemMoveDialog.newInstance(item).show(requireActivity(), ItemMoveDialog.TAG)
    }

    private fun pickDate(
        oldItem: FridgeItem,
        year: Int,
        month: Int,
        day: Int,
    ) {
        DateSelectDialog.newInstance(oldItem, year, month, day)
            .show(requireActivity(), DateSelectDialog.TAG)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        stateSaver?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        factory = null
        name = null
        date = null
        presence = null
        toolbar = null
        errorDisplay = null
        stateSaver = null
    }

    companion object {

        const val TAG = "ExpandedFragment"

        private const val ITEM = "item"
        private const val ENTRY = "entry"
        private const val PRESENCE = "presence"

        @JvmStatic
        @CheckResult
        fun createNew(
            entryId: FridgeEntry.Id,
            presence: Presence,
        ): DialogFragment {
            return newInstance(FridgeItem.Id.EMPTY, entryId, presence)
        }

        @JvmStatic
        @CheckResult
        fun openExisting(item: FridgeItem): DialogFragment {
            return newInstance(item.id(), item.entryId(), item.presence())
        }

        @JvmStatic
        @CheckResult
        private fun newInstance(
            itemId: FridgeItem.Id,
            entryId: FridgeEntry.Id,
            presence: Presence,
        ): DialogFragment {
            return ExpandedItemDialog().apply {
                arguments = Bundle().apply {
                    putString(ITEM, itemId.id)
                    putString(ENTRY, entryId.id)
                    putString(PRESENCE, presence.name)
                }
            }
        }
    }
}
