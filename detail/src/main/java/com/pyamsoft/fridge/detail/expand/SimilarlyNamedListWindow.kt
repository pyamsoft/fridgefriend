/*
 * Copyright 2020 Peter Kenji Yamanaka
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

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.annotation.CheckResult
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.databinding.SimilarlyNamedListItemBinding
import com.pyamsoft.pydroid.util.asDp
import timber.log.Timber

internal class SimilarlyNamedListWindow internal constructor(context: Context) {

    private val popupWindow = ListPopupWindow(context)
    private val adapter = SimilarlyNamedListAdapter()

    init {
        popupWindow.setAdapter(adapter)
        popupWindow.isModal = false
    }

    fun dismiss() {
        try {
            if (popupWindow.isShowing) {
                popupWindow.dismiss()
            }
        } catch (e: Exception) {
            Timber.w("Caught exception on dismiss: $e")
        }
    }

    private fun clear() {
        adapter.clear()
        popupWindow.height = 0
    }

    fun initializeView(anchor: View) {
        anchor.doOnLayout {
            popupWindow.anchorView = anchor
        }
    }

    private inline fun isRealItem(
        model: ExpandItemViewState.SimilarItem,
        func: (FridgeItem) -> Unit
    ) {
        if (model.item != null) {
            func(model.item)
        }
    }

    fun setOnItemClickListener(listener: (item: FridgeItem) -> Unit) {
        popupWindow.setOnItemClickListener { _, _, position, _ ->
            val model = adapter.getModel(position)
            isRealItem(model) { item ->
                listener(item)
                dismiss()
            }
        }
    }

    fun setOnDismissListener(listener: () -> Unit) {
        popupWindow.setOnDismissListener(listener)
    }

    fun teardown() {
        clear()
        dismiss()
        popupWindow.setOnItemClickListener(null)
        popupWindow.setOnDismissListener(null)
        popupWindow.setAdapter(null)
    }

    fun set(items: Collection<ExpandItemViewState.SimilarItem>, isFocused: Boolean) {
        adapter.set(items)

        if (isFocused) {
            show()
        } else {
            dismiss()
        }
    }

    fun show() {
        if (adapter.isEmpty) {
            dismiss()
        } else {
            popupWindow.show()
        }
    }

    private abstract class PopupWindowListAdapter protected constructor() : BaseAdapter() {

        private val fridgeItems = mutableListOf<ExpandItemViewState.SimilarItem>()

        @CheckResult
        fun getModel(position: Int): ExpandItemViewState.SimilarItem {
            return fridgeItems[position]
        }

        final override fun getItem(position: Int): Any {
            return getModel(position).also { Timber.d("SIMILAR: $it") }
        }

        final override fun getItemId(position: Int): Long {
            val model = getModel(position)
            val item = model.item
            return item?.id()?.hashCode()?.toLong() ?: 0
        }

        final override fun getCount(): Int {
            return fridgeItems.size
        }

        fun clear() {
            fridgeItems.clear()
        }

        fun set(items: Collection<ExpandItemViewState.SimilarItem>) {
            clear()
            fridgeItems.addAll(TITLE_ITEM + items)
            notifyDataSetChanged()
        }

        companion object {

            private val TITLE_ITEM = listOf(
                ExpandItemViewState.SimilarItem(item = null, display = "Similar Items")
            )

        }
    }

    private class SimilarlyNamedListAdapter : PopupWindowListAdapter() {

        @CheckResult
        private fun isTitle(model: ExpandItemViewState.SimilarItem): Boolean {
            return model.item == null
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = inflateView(convertView, parent)
            val holder = view.getViewHolder()
            val item = getModel(position)
            val isTitleModel = isTitle(item)

            val textView = holder.binding.similarlyNamedItemName

            // Set the text
            textView.text = item.display

            // Set the padding for title versus selectable item
            val horizontalPadding = 8.asDp(parent.context)
            val verticalPadding = (if (isTitleModel) 8 else 4).asDp(parent.context)
            holder.binding.root.updatePadding(
                left = horizontalPadding,
                right = horizontalPadding,
                top = verticalPadding,
                bottom = verticalPadding
            )

            // Set the text style for title versus selectable item
            textView.setTypeface(
                textView.typeface,
                if (isTitleModel) Typeface.BOLD else Typeface.NORMAL
            )

            return view
        }

        @CheckResult
        private fun inflateView(scrap: View?, parent: ViewGroup): View {
            val view: View
            if (scrap == null) {
                val inflater = LayoutInflater.from(parent.context)
                val binding = SimilarlyNamedListItemBinding.inflate(inflater, parent, false)
                view = binding.root
                view.tag = ViewHolder(binding)
            } else {
                view = scrap
            }

            return view
        }

        @CheckResult
        private fun View.getViewHolder(): ViewHolder {
            val tag = this.tag
            if (tag is ViewHolder) {
                return tag
            } else {
                throw IllegalStateException("View tag is not a ViewHolder: $tag")
            }
        }
    }

    private data class ViewHolder(val binding: SimilarlyNamedListItemBinding)
}
