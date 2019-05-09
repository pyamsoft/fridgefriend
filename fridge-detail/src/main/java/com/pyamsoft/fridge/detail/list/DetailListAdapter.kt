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
import com.pyamsoft.fridge.detail.item.add.AddNewItemUiComponent
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemUiComponent
import com.pyamsoft.fridge.detail.list.DetailListAdapter.DetailViewHolder
import com.pyamsoft.pydroid.arch.layout
import com.pyamsoft.pydroid.util.toDp
import timber.log.Timber
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

    @JvmField @Inject internal var component: DetailListItemUiComponent? = null

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

      val component = requireNotNull(component)
      component.bind(parent, owner, null, object : DetailListItemUiComponent.Callback {

        override fun onLastDoneClicked() {
        }

        override fun onExpandItem(item: FridgeItem) {
          callback.onItemExpanded(item)
        }

      })

      parent.layout {
        component.also {
          connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
          connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
          connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
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
      component = null
    }

    // Kind of hacky
    fun archiveSelf() {
      requireNotNull(component).archiveSelf()
    }

    // Very hacky
    fun focus() {
      Timber.d("Request focus onto item")
      requireNotNull(component).requestFocus()
    }

  }

  internal class AddNewItemViewHolder internal constructor(
    view: View,
    private val factory: (parent: ViewGroup, item: FridgeItem, editable: Boolean) -> DetailItemComponent
  ) : DetailViewHolder(view) {

    @JvmField @Inject internal var component: AddNewItemUiComponent? = null

    private val parent: ViewGroup = itemView.findViewById(R.id.listitem_frame)

    private var lifecycle: ListItemLifecycle? = null
    private var callback: Callback? = null

    fun bind(
      item: FridgeItem,
      cb: Callback
    ) {
      callback = cb
      lifecycle?.unbind()

      factory(parent, item, false)
          .inject(this)

      val owner = ListItemLifecycle()
      lifecycle = owner
      requireNotNull(component).bind(owner, null, object : AddNewItemUiComponent.Callback {

        override fun onAddNewItem() {
          requireNotNull(callback).onItemExpanded(FridgeItem.create(entryId = item.entryId()))
        }

      })
      owner.bind()
    }

    override fun unbind() {
      lifecycle?.unbind()
      lifecycle = null
      callback = null
      component = null
    }
  }

  interface Callback {

    fun onItemExpanded(item: FridgeItem)

  }

}


