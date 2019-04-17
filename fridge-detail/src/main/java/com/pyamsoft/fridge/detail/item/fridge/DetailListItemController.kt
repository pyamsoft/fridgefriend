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

package com.pyamsoft.fridge.detail.item.fridge

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.DetailItem
import com.pyamsoft.fridge.detail.item.DetailItemComponent
import com.pyamsoft.fridge.detail.item.ListItemLifecycle
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemController.ViewHolder
import com.pyamsoft.pydroid.arch.layout
import com.pyamsoft.pydroid.util.toDp
import timber.log.Timber
import javax.inject.Inject

internal class DetailListItemController internal constructor(
  item: FridgeItem,
  editable: Boolean,
  private val factory: (parent: ViewGroup, item: FridgeItem, editable: Boolean) -> DetailItemComponent,
  private val callback: Callback
) : DetailItem<DetailListItemController, ViewHolder>(item, swipeable = editable) {

  override fun getType(): Int {
    return R.id.id_item_list_item
  }

  override fun getViewHolder(v: View): ViewHolder {
    val horizontalPadding = 16.toDp(v.context)
    val verticalPadding = 8.toDp(v.context)
    v.updatePadding(
      left = horizontalPadding,
      right = horizontalPadding,
      top = verticalPadding,
      bottom = verticalPadding
    )
    return ViewHolder(v, factory)
  }

  override fun getLayoutRes(): Int {
    return R.layout.listitem_constraint
  }

  override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
    super.bindView(holder, payloads)
    holder.bind(model, canSwipe(), callback)
  }

  override fun unbindView(holder: ViewHolder) {
    super.unbindView(holder)
    holder.unbind()
  }

  class ViewHolder internal constructor(
    itemView: View,
    private val factory: (parent: ViewGroup, item: FridgeItem, editable: Boolean) -> DetailItemComponent
  ) : RecyclerView.ViewHolder(itemView), DetailListItemUiComponent.Callback {

    @field:Inject internal lateinit var component: DetailListItemUiComponent

    private val parent: ConstraintLayout = itemView.findViewById(R.id.listitem_constraint)

    private var lifecycle: ListItemLifecycle? = null
    private var callback: Callback? = null

    override fun onLastDoneClicked() {
      requireNotNull(callback).onLastDoneClicked(layoutPosition)
    }

    fun bind(item: FridgeItem, editable: Boolean, cb: Callback) {
      callback = cb
      lifecycle?.unbind()

      factory(parent, item, editable)
        .inject(this)

      val owner = ListItemLifecycle()
      lifecycle = owner
      component.bind(parent, owner, null, this)

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

    fun unbind() {
      lifecycle?.unbind()
      lifecycle = null
      callback = null
    }

    // Kind of hacky
    fun deleteSelf(item: FridgeItem) {
      Timber.d("Delete self: $item")
      component.deleteSelf(item)
    }

    // Very hacky
    fun focus() {
      Timber.d("Request focus onto item")
      component.requestFocus()
    }

  }

  interface Callback {

    fun onLastDoneClicked(position: Int)

  }

}