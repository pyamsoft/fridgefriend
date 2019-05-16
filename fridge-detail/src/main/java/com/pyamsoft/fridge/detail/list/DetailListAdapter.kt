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

package com.pyamsoft.fridge.detail.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.DetailItemComponent
import com.pyamsoft.fridge.detail.item.ListItemLifecycle
import com.pyamsoft.fridge.detail.item.add.AddNewControllerEvent.AddNew
import com.pyamsoft.fridge.detail.item.add.AddNewItemView
import com.pyamsoft.fridge.detail.item.add.AddNewItemViewModel
import com.pyamsoft.fridge.detail.item.fridge.DetailItemControllerEvent.DatePick
import com.pyamsoft.fridge.detail.item.fridge.DetailItemControllerEvent.ExpandDetails
import com.pyamsoft.fridge.detail.item.fridge.DetailItemViewModel
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemDate
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemName
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemPresence
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemStrikethrough
import com.pyamsoft.fridge.detail.list.DetailListAdapter.DetailViewHolder
import com.pyamsoft.pydroid.arch.impl.createComponent
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.util.toDp
import javax.inject.Inject

internal class DetailListAdapter constructor(
  private val editable: Boolean,
  private val factory: (parent: ViewGroup, item: FridgeItem, editable: Boolean) -> DetailItemComponent,
  private val callback: Callback
) : ListAdapter<FridgeItem, DetailViewHolder>(object : DiffUtil.ItemCallback<FridgeItem>() {

  override fun areItemsTheSame(
    oldItem: FridgeItem,
    newItem: FridgeItem
  ): Boolean {
    return oldItem.id() == newItem.id()
  }

  override fun areContentsTheSame(
    oldItem: FridgeItem,
    newItem: FridgeItem
  ): Boolean {
    return JsonMappableFridgeItem.from(oldItem) == JsonMappableFridgeItem.from(newItem)
  }

}) {

  override fun getItemViewType(position: Int): Int {
    if (getItem(position).id().isBlank()) {
      return R.id.id_item_add_new_item
    } else {
      return R.id.id_item_list_item
    }
  }

  override fun getItemId(position: Int): Long {
    return getItem(position).id()
        .hashCode()
        .toLong()
  }

  @CheckResult
  private fun View.fixPadding(): View {
    val horizontalPadding = 16.toDp(this.context)
    val verticalPadding = 8.toDp(this.context)
    this.updatePadding(
        left = horizontalPadding,
        right = horizontalPadding,
        top = verticalPadding,
        bottom = verticalPadding
    )
    return this
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): DetailViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    if (viewType == R.id.id_item_add_new_item) {
      val v = inflater.inflate(R.layout.listitem_frame, parent, false)
      return AddNewItemViewHolder(v, factory)
    } else {
      val v = inflater.inflate(R.layout.listitem_constraint, parent, false)
          .fixPadding()
      return DetailItemViewHolder(v, factory)
    }
  }

  override fun onBindViewHolder(
    holder: DetailViewHolder,
    position: Int
  ) {
    val item = getItem(position)
    if (item.id().isBlank()) {
      (holder as AddNewItemViewHolder).bind(item, callback)
    } else {
      (holder as DetailItemViewHolder).bind(item, editable, callback)
    }
  }

  override fun onViewRecycled(holder: DetailViewHolder) {
    super.onViewRecycled(holder)
    holder.unbind()
  }

  internal abstract class DetailViewHolder protected constructor(
    view: View
  ) : RecyclerView.ViewHolder(view) {

    abstract fun unbind()

  }

  internal class DetailItemViewHolder internal constructor(
    itemView: View,
    private val factory: (parent: ViewGroup, item: FridgeItem, editable: Boolean) -> DetailItemComponent
  ) : DetailViewHolder(itemView) {

    @JvmField @Inject internal var viewModel: DetailItemViewModel? = null
    @JvmField @Inject internal var name: DetailListItemName? = null
    @JvmField @Inject internal var date: DetailListItemDate? = null
    @JvmField @Inject internal var presence: DetailListItemPresence? = null
    @JvmField @Inject internal var strikethrough: DetailListItemStrikethrough? = null

    private val parent: ConstraintLayout = itemView.findViewById(R.id.listitem_constraint)

    private var lifecycle: ListItemLifecycle? = null

    fun bind(
      item: FridgeItem,
      editable: Boolean,
      callback: Callback
    ) {
      lifecycle?.unbind()

      factory(parent, item, editable)
          .inject(this)

      val owner = ListItemLifecycle()
      lifecycle = owner

      val name = requireNotNull(name)
      val date = requireNotNull(date)
      val presence = requireNotNull(presence)
      val strikethrough = requireNotNull(strikethrough)
      createComponent(
          null, owner,
          requireNotNull(viewModel),
          name,
          date,
          presence,
          strikethrough
      ) {
        return@createComponent when (it) {
          is ExpandDetails -> callback.onItemExpanded(it.item)
          is DatePick -> callback.onPickDate(it.oldItem, it.year, it.month, it.day)
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

        strikethrough.also {
          connect(it.id(), ConstraintSet.START, presence.id(), ConstraintSet.END)
          connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
          connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
          connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
          constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
          constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        }

      }

      owner.bind()
    }

    override fun unbind() {
      lifecycle?.unbind()
      lifecycle = null
    }

    // Kind of hacky
    fun archiveSelf() {
      requireNotNull(viewModel).archiveSelf()
    }

  }

  internal class AddNewItemViewHolder internal constructor(
    view: View,
    private val factory: (parent: ViewGroup, item: FridgeItem, editable: Boolean) -> DetailItemComponent
  ) : DetailViewHolder(view) {

    @JvmField @Inject internal var view: AddNewItemView? = null
    @JvmField @Inject internal var viewModel: AddNewItemViewModel? = null

    private val parent: ViewGroup = itemView.findViewById(R.id.listitem_frame)

    private var lifecycle: ListItemLifecycle? = null

    fun bind(
      item: FridgeItem,
      callback: Callback
    ) {
      lifecycle?.unbind()

      factory(parent, item, false)
          .inject(this)

      val owner = ListItemLifecycle()
      lifecycle = owner

      createComponent(
          null, owner,
          requireNotNull(viewModel),
          requireNotNull(view)
      ) {
        return@createComponent when (it) {
          is AddNew -> addNewItem(it.entryId, callback)
        }
      }
      owner.bind()
    }

    private fun addNewItem(
      entryId: String,
      callback: Callback
    ) {
      callback.onItemExpanded(FridgeItem.create(entryId = entryId))
    }

    override fun unbind() {
      lifecycle?.unbind()
      lifecycle = null

      view = null
      viewModel = null
    }
  }

  interface Callback {

    fun onItemExpanded(item: FridgeItem)

    fun onPickDate(
      oldItem: FridgeItem,
      year: Int,
      month: Int,
      day: Int
    )

  }

}


