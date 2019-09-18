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

package com.pyamsoft.fridge.detail.expand

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.appcompat.widget.ListPopupWindow
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R

internal class SimilarlyNamedListWindow internal constructor(
    context: Context,
    onItemClicked: (item: FridgeItem) -> Unit,
    onDismissed: () -> Unit
) : ListPopupWindow(context) {

    private val adapter = SimilarlyNamedListAdapter()

    init {
        setAdapter(adapter)
        isModal = true

        setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getFridgeItem(position)
            onItemClicked(item)
            dismiss()
        }

        setOnDismissListener(onDismissed)
    }

    fun initializeView(anchor: View) {
        anchor.post {
            anchorView = anchor
            width = anchor.width
        }
    }

    fun clear() {
        adapter.clear()
        height = 0
    }

    fun add(item: FridgeItem) {
        add(listOf(item))
    }

    fun add(vararg items: FridgeItem) {
        add(items.asList())
    }

    internal fun add(items: List<FridgeItem>) {
        adapter.add(items)
        height = adapter.count * ITEM_HEIGHT
    }

    private abstract class FridgeItemListAdapter protected constructor() : BaseAdapter() {

        private val fridgeItems: MutableList<FridgeItem> = arrayListOf()

        @CheckResult
        internal fun getFridgeItem(position: Int): FridgeItem {
            return fridgeItems[position]
        }

        final override fun getItem(position: Int): Any {
            return getFridgeItem(position)
        }

        final override fun getItemId(position: Int): Long {
            return getFridgeItem(position).id().hashCode().toLong()
        }

        final override fun getCount(): Int {
            return fridgeItems.size
        }

        internal fun clear() {
            fridgeItems.clear()
            notifyDataSetChanged()
        }

        internal fun add(items: List<FridgeItem>) {
            fridgeItems.addAll(items)
            notifyDataSetChanged()
        }
    }

    private class SimilarlyNamedListAdapter internal constructor() : FridgeItemListAdapter() {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = inflateView(convertView, parent)
            val holder = view.getViewHolder()
            val item = getFridgeItem(position)
            holder.name.text = item.name()
            return view
        }

        @CheckResult
        private fun inflateView(scrap: View?, parent: ViewGroup): View {
            val view: View
            if (scrap == null) {
                val inflater = LayoutInflater.from(parent.context)
                view = inflater.inflate(R.layout.similarly_named_list_item, parent, false)

                val nameView = view.findViewById<TextView>(R.id.similarly_named_item_name)
                view.tag = ViewHolder(nameView)
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

    private data class ViewHolder internal constructor(val name: TextView)

    companion object {

        private const val ITEM_HEIGHT = 48
    }
}
