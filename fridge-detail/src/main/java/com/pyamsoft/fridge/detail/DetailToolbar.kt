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

package com.pyamsoft.fridge.detail

import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.CheckResult
import androidx.annotation.IdRes
import androidx.appcompat.widget.Toolbar
import com.pyamsoft.fridge.detail.DetailViewEvent.ArchiveEntry
import com.pyamsoft.fridge.detail.DetailViewEvent.CloseEntry
import com.pyamsoft.fridge.detail.DetailViewEvent.ToggleArchiveVisibility
import com.pyamsoft.pydroid.arch.UiView
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import com.pyamsoft.pydroid.ui.arch.InvalidIdException
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import javax.inject.Inject

class DetailToolbar @Inject internal constructor(
  private val toolbarActivity: ToolbarActivity
) : UiView<DetailViewState, DetailViewEvent>() {

  private var deleteMenuItem: MenuItem? = null
  private var showArcivedMenuItem: MenuItem? = null
  private var hideArchivedMenuItem: MenuItem? = null

  override fun id(): Int {
    throw InvalidIdException
  }

  override fun inflate(savedInstanceState: Bundle?) {
    toolbarActivity.requireToolbar { toolbar ->
      toolbar.setUpEnabled(true)
      toolbar.setNavigationOnClickListener(DebouncedOnClickListener.create {
        publish(CloseEntry)
      })

      toolbar.inflateMenu(R.menu.menu_detail)

      deleteMenuItem = toolbar.findMenuItem(R.id.menu_item_delete) { item ->
        item.setOnMenuItemClickListener {
          publish(ArchiveEntry)
          return@setOnMenuItemClickListener true
        }
      }

      showArcivedMenuItem = toolbar.findMenuItem(R.id.menu_item_show_archived) { item ->
        item.setOnMenuItemClickListener {
          publish(ToggleArchiveVisibility(true))
          return@setOnMenuItemClickListener true
        }
      }

      hideArchivedMenuItem = toolbar.findMenuItem(R.id.menu_item_hide_archived) { item ->
        item.setOnMenuItemClickListener {
          publish(ToggleArchiveVisibility(false))
          return@setOnMenuItemClickListener true
        }
      }
    }
  }

  @CheckResult
  private inline fun Toolbar.findMenuItem(
    @IdRes menuItemId: Int,
    onFound: (item: MenuItem) -> Unit
  ): MenuItem {
    val item = this.menu.findItem(menuItemId)
    item.isVisible = false
    onFound(item)
    return item
  }

  override fun teardown() {
    toolbarActivity.withToolbar { toolbar ->
      toolbar.setUpEnabled(false)
      toolbar.setNavigationOnClickListener(null)

      deleteMenuItem?.setOnMenuItemClickListener(null)
      showArcivedMenuItem?.setOnMenuItemClickListener(null)
      hideArchivedMenuItem?.setOnMenuItemClickListener(null)

      deleteMenuItem = null
      showArcivedMenuItem = null
      hideArchivedMenuItem = null

      toolbar.menu.removeItem(R.id.menu_item_hide_archived)
      toolbar.menu.removeItem(R.id.menu_item_show_archived)
      toolbar.menu.removeItem(R.id.menu_item_delete)
    }
  }

  private fun setDeleteEnabled(real: Boolean) {
    deleteMenuItem?.isVisible = real
  }

  override fun render(
    state: DetailViewState,
    oldState: DetailViewState?
  ) {
    setDeleteEnabled(state.entry.isReal())

    state.filterArchived.let { hideArchived ->
      showArcivedMenuItem?.isVisible = hideArchived
      hideArchivedMenuItem?.isVisible = !hideArchived
    }
  }

}
