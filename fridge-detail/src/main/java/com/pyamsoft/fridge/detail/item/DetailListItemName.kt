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

package com.pyamsoft.fridge.detail.item

import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.ExpandItem
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class DetailListItemName @Inject internal constructor(
  parent: ViewGroup
) : BaseUiView<DetailItemViewState, DetailItemViewEvent>(parent) {

  override val layout: Int = R.layout.detail_list_item_name

  override val layoutRoot by boundView<ViewGroup>(R.id.detail_item_name)

  private val nameView by boundView<EditText>(R.id.detail_item_name_editable)

  override fun onRender(
    state: DetailItemViewState,
    savedInstanceState: Bundle?
  ) {
    state.item.let { item ->
      nameView.setTextKeepState(item.name())
      nameView.setNotEditable()
      nameView.setOnDebouncedClickListener {
        publish(ExpandItem(item))
      }
    }
  }

  override fun onTeardown() {
    nameView.text.clear()
    nameView.setOnDebouncedClickListener(null)
  }
}

