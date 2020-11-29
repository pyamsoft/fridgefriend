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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.annotation.CheckResult
import androidx.appcompat.widget.ListPopupWindow
import androidx.core.view.doOnLayout
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.databinding.SimilarlyNamedListItemBinding
import com.pyamsoft.fridge.detail.databinding.SimilarlyNamedListTitleBinding
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

    private inline fun withRealItem(
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
            withRealItem(model) { item ->
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
        private fun isTitle(position: Int): Boolean {
            return position == 0
        }

        @CheckResult
        private fun isTitle(model: ExpandItemViewState.SimilarItem): Boolean {
            return model.item == null
        }

        private fun bindItem(view: View, model: ExpandItemViewState.SimilarItem) {
            val holder = view.requireItemViewHolder()
            holder.binding.similarlyNamedItemName.text = model.display
        }

        private fun bindTitle(view: View, model: ExpandItemViewState.SimilarItem) {
            val holder = view.requireTitleViewHolder()
            holder.binding.similarlyNamedItemTitle.text = model.display
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = inflateView(position, convertView, parent)
            val model = getModel(position)
            if (isTitle(model)) {
                bindTitle(view, model)
            } else {
                bindItem(view, model)
            }

            return view
        }

        @CheckResult
        private fun inflateNewView(position: Int, parent: ViewGroup): View {
            return if (isTitle(position)) {
                Timber.d("Inflating new title view into null scrap")
                inflateTitleView(parent)
            } else {
                Timber.d("Inflating new item view into null scrap")
                inflateItemView(parent)
            }
        }

        @CheckResult
        private fun View.recycleView(position: Int, parent: ViewGroup): View {
            return if (isTitle(position)) {
                if (this.getTitleViewHolder() != null) this else {
                    Timber.d("Inflating new title view into recycled scrap")
                    inflateTitleView(parent)
                }
            } else {
                if (this.getItemViewHolder() != null) this else {
                    Timber.d("Inflating new item view into recycled scrap")
                    inflateItemView(parent)
                }
            }
        }

        @CheckResult
        private fun inflateView(position: Int, scrap: View?, parent: ViewGroup): View {
            return scrap?.recycleView(position, parent) ?: inflateNewView(position, parent)
        }

        @CheckResult
        private fun inflateItemView(parent: ViewGroup): View {
            val inflater = LayoutInflater.from(parent.context)
            val binding = SimilarlyNamedListItemBinding.inflate(inflater, parent, false)
            return binding.root.apply {
                tag = ItemViewHolder(binding)
            }
        }

        @CheckResult
        private fun inflateTitleView(parent: ViewGroup): View {
            val inflater = LayoutInflater.from(parent.context)
            val binding = SimilarlyNamedListTitleBinding.inflate(inflater, parent, false)
            return binding.root.apply {
                tag = TitleViewHolder(binding)
            }
        }

        @CheckResult
        private fun View.requireItemViewHolder(): ItemViewHolder {
            return this.requireViewHolder()
        }

        @CheckResult
        private fun View.requireTitleViewHolder(): TitleViewHolder {
            return this.requireViewHolder()
        }

        @CheckResult
        private inline fun <reified S : Any> View.requireViewHolder(): S {
            return this.getViewHolder<S>()
                ?: throw IllegalStateException("View is not ViewHolder: ${this.tag}")
        }

        @CheckResult
        private fun View.getItemViewHolder(): ItemViewHolder? {
            return this.getViewHolder()
        }

        @CheckResult
        private fun View.getTitleViewHolder(): TitleViewHolder? {
            return this.getViewHolder()
        }

        @CheckResult
        private inline fun <reified S : Any> View.getViewHolder(): S? {
            val tag = this.tag
            return if (tag is S) tag else null
        }
    }

    private data class ItemViewHolder(val binding: SimilarlyNamedListItemBinding)

    private data class TitleViewHolder(val binding: SimilarlyNamedListTitleBinding)
}
