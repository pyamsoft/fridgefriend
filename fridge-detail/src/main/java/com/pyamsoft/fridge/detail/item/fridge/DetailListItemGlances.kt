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

import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.core.view.isVisible
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.fridge.DetailItemViewEvent.ExpandItem
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import java.util.Date
import javax.inject.Inject

class DetailListItemGlances @Inject internal constructor(
  parent: ViewGroup
) : BaseUiView<DetailItemViewState, DetailItemViewEvent>(parent) {

  override val layout: Int = R.layout.detail_list_item_glances

  override val layoutRoot by boundView<ViewGroup>(R.id.detail_item_glances)

  private val validExpirationDate by boundView<CompoundButton>(R.id.detail_item_glances_date)
  private val itemExpired by boundView<CompoundButton>(R.id.detail_item_glances_expired)

  override fun onRender(
    state: DetailItemViewState,
    oldState: DetailItemViewState?
  ) {
    state.item.let { item ->
      val isVisible = item.isReal() && !item.isArchived() && item.presence() == HAVE
      layoutRoot.isVisible = isVisible

      layoutRoot.setOnDebouncedClickListener {
        publish(ExpandItem(item))
      }

      val now = Date()
      val hasExpirationDate = item.expireTime() != FridgeItem.EMPTY_EXPIRE_TIME
      validExpirationDate.isChecked = hasExpirationDate
      itemExpired.isChecked = if (hasExpirationDate) item.expireTime().before(now) else false
    }
  }

  override fun onTeardown() {
    layoutRoot.setOnDebouncedClickListener(null)
  }

}
