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

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.tintWith

internal class AddNewListItem internal constructor(
  item: FridgeItem,
  private val theming: Theming,
  private val imageLoader: ImageLoader,
  private val callback: AddNewListItem.Callback
) : DetailItem<AddNewListItem, AddNewListItem.ViewHolder>(item) {

  override fun getType(): Int {
    return R.id.id_item_add_new_item
  }

  override fun getViewHolder(v: View): ViewHolder {
    return ViewHolder(v, theming, imageLoader, callback)
  }

  override fun getLayoutRes(): Int {
    return R.layout.add_new_list_item
  }

  override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
    super.bindView(holder, payloads)
    holder.bind()
  }

  override fun unbindView(holder: ViewHolder) {
    super.unbindView(holder)
    holder.unbind()
  }

  override fun finalCommitOnDestroy() {
  }

  class ViewHolder internal constructor(
    itemView: View,
    private val theming: Theming,
    private val imageLoader: ImageLoader,
    private val callback: AddNewListItem.Callback
  ) : RecyclerView.ViewHolder(itemView) {

    private val addNewIcon = itemView.findViewById<ImageView>(R.id.detail_add_new_item_icon)

    private var iconLoaded: Loaded? = null

    fun bind() {
      disposeIcon()
      iconLoaded = imageLoader.load(R.drawable.ic_add_24dp)
        .mutate { drawable ->
          val color: Int
          if (theming.isDarkTheme()) {
            color = R.color.white
          } else {
            color = R.color.black
          }
          return@mutate drawable.tintWith(itemView.context, color)
        }.into(addNewIcon)

      itemView.setOnDebouncedClickListener { callback.onAddNewClicked() }
    }

    fun unbind() {
      disposeIcon()
      addNewIcon.setImageDrawable(null)
      itemView.setOnClickListener(null)
    }

    private fun disposeIcon() {
      iconLoaded?.dispose()
      iconLoaded = null
    }
  }

  interface Callback {

    fun onAddNewClicked()

  }

}