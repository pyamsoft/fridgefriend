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

package com.pyamsoft.fridge.detail.create.toolbar

import android.os.Bundle
import android.view.MenuItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.create.toolbar.CreationToolbarViewEvent.Archive
import com.pyamsoft.fridge.detail.create.toolbar.CreationToolbarViewEvent.Close
import com.pyamsoft.fridge.detail.toolbar.DetailToolbar
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import javax.inject.Inject

class CreationToolbar @Inject internal constructor(
  toolbarActivity: ToolbarActivity
) : DetailToolbar<CreationToolbarViewState, CreationToolbarViewEvent>(toolbarActivity, { Close }) {

  private var deleteMenuItem: MenuItem? = null

  override fun onInflate(savedInstanceState: Bundle?) {
    toolbarActivity.requireToolbar { toolbar ->
      toolbar.inflateMenu(R.menu.menu_detail)
      val deleteItem = toolbar.menu.findItem(R.id.menu_item_delete)
      deleteItem.isVisible = false
      deleteItem.setOnMenuItemClickListener {
        publish(Archive)
        return@setOnMenuItemClickListener true
      }
      deleteMenuItem = deleteItem
    }
  }

  override fun onTeardown() {
    toolbarActivity.withToolbar { toolber ->
      deleteMenuItem?.setOnMenuItemClickListener(null)
      deleteMenuItem = null
      toolber.menu.removeItem(R.id.menu_item_delete)
    }
  }

  private fun setDeleteEnabled(real: Boolean) {
    deleteMenuItem?.isVisible = real
  }

  override fun render(
    state: CreationToolbarViewState,
    oldState: CreationToolbarViewState?
  ) {
    setDeleteEnabled(state.isReal)
  }

}
